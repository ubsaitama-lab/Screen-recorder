package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@SuppressLint("MissingPermission")
class AdvancedScreenRecorder(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val outputFile: File,
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val videoBitrate: Int,
    private val recordInternalAudio: Boolean,
    private val recordMicAudio: Boolean,
    private val useHevc: Boolean
) {
    private var videoCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var audioRecord: AudioRecord? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    @Volatile private var muxerStarted = false
    private val isRecording = AtomicBoolean(false)

    private var videoThread: Thread? = null
    private var audioThread: Thread? = null

    private val TAG = "AdvancedScreenRecorder"

    fun start() {
        if (isRecording.get()) return
        isRecording.set(true)

        try {
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            throw RuntimeException("Muxer creation failed: ${e.message}")
        }
        
        try {
            setupVideo()
        } catch (e: Exception) {
            throw RuntimeException("Video setup failed: ${e.message}")
        }
        
        if (recordInternalAudio || recordMicAudio) {
            try {
                setupAudio()
            } catch (e: Exception) {
                Log.e(TAG, "Audio setup failed, continuing without audio", e)
                // Fallback to video only
                audioCodec = null
                audioRecord = null
                mediaMuxer?.start()
                muxerStarted = true
            }
        } else {
            mediaMuxer?.start()
            muxerStarted = true
        }

        videoThread = thread { videoEncodeLoop() }
        if (audioCodec != null) {
            audioThread = thread { audioEncodeLoop() }
        }
    }

    private fun setupVideo() {
        var mime = if (useHevc) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
        
        var format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        
        try {
            videoCodec = MediaCodec.createEncoderByType(mime)
            videoCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure video codec for $mime, falling back to AVC", e)
            mime = MediaFormat.MIMETYPE_VIDEO_AVC
            
            // Just drop to 30fps and standard bitrate but keep dimensions, as wrong dimensions will break surface
            format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 5000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoCodec = MediaCodec.createEncoderByType(mime)
            videoCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        val surface = videoCodec?.createInputSurface()
        videoCodec?.start()

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenRecorder", format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT), context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
        )
    }

    private fun setupAudio() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && recordInternalAudio) {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
        } else {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        }

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
        }
        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioCodec?.start()
    }

    private fun videoEncodeLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRecording.get()) {
            val outputBufferId = try { videoCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1 } catch (e: Exception) { -1 }
            if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = videoCodec?.outputFormat
                if (newFormat != null) {
                    synchronized(this) {
                        videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                        checkMuxerStart()
                    }
                }
            } else if (outputBufferId >= 0) {
                val encodedData = videoCodec?.getOutputBuffer(outputBufferId)
                if (encodedData != null && muxerStarted) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        try {
                            mediaMuxer?.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        } catch (e: Exception) { Log.e(TAG, "Video mux error", e) }
                    }
                }
                try { videoCodec?.releaseOutputBuffer(outputBufferId, false) } catch (e: Exception) {}
            }
        }
    }

    private fun audioEncodeLoop() {
        try { audioRecord?.startRecording() } catch (e: Exception) { return }
        val bufferInfo = MediaCodec.BufferInfo()
        val bufferSize = 1024 * 4
        val audioBuffer = ByteArray(bufferSize)

        while (isRecording.get()) {
            val readResult = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
            if (readResult > 0) {
                val inputBufferId = try { audioCodec?.dequeueInputBuffer(10000) ?: -1 } catch (e: Exception) { -1 }
                if (inputBufferId >= 0) {
                    val inputBuffer = audioCodec?.getInputBuffer(inputBufferId)
                    inputBuffer?.clear()
                    inputBuffer?.put(audioBuffer, 0, readResult)
                    val pts = System.nanoTime() / 1000
                    try { audioCodec?.queueInputBuffer(inputBufferId, 0, readResult, pts, 0) } catch (e: Exception) {}
                }
            }

            var outputBufferId = try { audioCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1 } catch (e: Exception) { -1 }
            while (outputBufferId >= 0) {
                if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = audioCodec?.outputFormat
                    if (newFormat != null) {
                        synchronized(this) {
                            audioTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                            checkMuxerStart()
                        }
                    }
                } else {
                    val encodedData = audioCodec?.getOutputBuffer(outputBufferId)
                    if (encodedData != null && muxerStarted) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            try {
                                mediaMuxer?.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            } catch (e: Exception) { Log.e(TAG, "Audio mux error", e) }
                        }
                    }
                    try { audioCodec?.releaseOutputBuffer(outputBufferId, false) } catch (e: Exception) {}
                }
                outputBufferId = try { audioCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1 } catch (e: Exception) { -1 }
            }
        }
        try { audioRecord?.stop() } catch (e: Exception) {}
    }

    private fun checkMuxerStart() {
        if (!muxerStarted) {
            val videoReady = videoTrackIndex >= 0
            val audioReady = if (audioCodec != null) audioTrackIndex >= 0 else true
            if (videoReady && audioReady) {
                try {
                    mediaMuxer?.start()
                    muxerStarted = true
                } catch (e: Exception) {
                    Log.e(TAG, "Muxer start failed", e)
                }
            }
        }
    }

    fun stop() {
        isRecording.set(false)
        try { videoThread?.join(1000) } catch (_: Exception) {}
        try { audioThread?.join(1000) } catch (_: Exception) {}

        try {
            videoCodec?.stop()
            videoCodec?.release()
        } catch (_: Exception) {}

        try {
            audioCodec?.stop()
            audioCodec?.release()
        } catch (_: Exception) {}

        try {
            audioRecord?.release()
        } catch (_: Exception) {}

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}

        try {
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (_: Exception) {}
    }
}
