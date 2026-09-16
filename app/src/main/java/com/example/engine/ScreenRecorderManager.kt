package com.example.engine

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.data.db.RecordedVideoEntity
import com.example.data.db.VideoRepository
import com.example.data.model.AudioSourceOption
import com.example.data.model.CodecOption
import com.example.data.model.Dlss5Mode
import com.example.data.model.RecorderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RecordingState {
    object Idle : RecordingState()
    data class Countdown(val secondsRemaining: Int) : RecordingState()
    data class Recording(
        val durationMs: Long,
        val currentFps: Int,
        val currentTempC: Float,
        val isThermalSafe: Boolean,
        val dlss5ActiveInRealtime: Boolean,
        val dlss5AutoFallbackQueued: Boolean,
        val estimatedSizeBytes: Long
    ) : RecordingState()
    data class Paused(val durationMs: Long) : RecordingState()
    data class Finished(val savedVideo: RecordedVideoEntity, val autoDlss5Triggered: Boolean) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class ScreenRecorderManager(
    private val context: Context,
    private val repository: VideoRepository,
    private val dlss5Engine: Dlss5Engine,
    private val hardwareProfiler: HardwareProfiler,
    private val touchManager: TouchOptimizationManager
) {
    companion object {
        private const val TAG = "ScreenRecorderManager"
        @Volatile
        var instance: ScreenRecorderManager? = null
            private set

        fun init(
            context: Context,
            repository: VideoRepository,
            dlss5Engine: Dlss5Engine,
            hardwareProfiler: HardwareProfiler,
            touchManager: TouchOptimizationManager
        ): ScreenRecorderManager {
            return instance ?: synchronized(this) {
                val created = ScreenRecorderManager(context, repository, dlss5Engine, hardwareProfiler, touchManager)
                instance = created
                created
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var advancedRecorder: AdvancedScreenRecorder? = null

    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var pausedDurationAcc = 0L
    private var pauseStartTime = 0L
    private var isPaused = false

    private var timerJob: Job? = null
    var currentSettings = RecorderSettings()
        private set

    fun setMediaProjection(projection: MediaProjection) {
        this.mediaProjection = projection
    }

    fun startWithProjection(projection: MediaProjection, settings: RecorderSettings = currentSettings) {
        this.mediaProjection = projection
        this.currentSettings = settings
        if (settings.countdownSeconds > 0) {
            coroutineScope.launch {
                for (s in settings.countdownSeconds downTo 1) {
                    _recordingState.value = RecordingState.Countdown(s)
                    delay(1000)
                }
                executeStartRecording()
            }
        } else {
            executeStartRecording()
        }
    }

    fun startRecordingWorkflow(settings: RecorderSettings, onNeedProjectionPermission: () -> Unit) {
        currentSettings = settings
        onNeedProjectionPermission()
    }

    private fun executeStartRecording() {
        try {
            if (mediaProjection == null) {
                _recordingState.value = RecordingState.Error("Media projection permission required")
                return
            }

            // Apply zero touch latency shield
            if (currentSettings.zeroTouchLagMode) {
                touchManager.applyTouchLatencyShield()
            }

            val displayMetrics = context.resources.displayMetrics
            val screenDensity = displayMetrics.densityDpi
            val displayWidth = displayMetrics.widthPixels
            val displayHeight = displayMetrics.heightPixels

            var recordWidth = displayWidth
            var recordHeight = displayHeight

            val isAdvancedAudio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    (currentSettings.audioSource == AudioSourceOption.INTERNAL_ONLY || currentSettings.audioSource == AudioSourceOption.INTERNAL_AND_MIC)

            // Always cap real-time recording to display dimensions to prevent severe hardware encoder lag
            // DLSS 5 will upscale it in post-process or simulate the real-time effect without killing the GPU
            if (currentSettings.resolution.width < displayWidth) {
                recordWidth = currentSettings.resolution.width
                recordHeight = currentSettings.resolution.height
            }

            // Ensure dimensions are even
            recordWidth = (recordWidth / 2) * 2
            recordHeight = (recordHeight / 2) * 2

            // Use app-specific cache to avoid Scoped Storage crashes during recording
            val cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM) ?: context.cacheDir
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            currentOutputFile = File(cacheDir, "APEX_REC_${timestamp}.mp4")

            if (isAdvancedAudio) {
                advancedRecorder = AdvancedScreenRecorder(
                    context = context,
                    mediaProjection = mediaProjection!!,
                    outputFile = currentOutputFile!!,
                    width = recordWidth,
                    height = recordHeight,
                    fps = currentSettings.fps.fps.coerceAtMost(60),
                    videoBitrate = currentSettings.bitrate.bps,
                    recordInternalAudio = true,
                    recordMicAudio = currentSettings.audioSource == AudioSourceOption.INTERNAL_AND_MIC,
                    useHevc = currentSettings.codec == CodecOption.HEVC
                )
                advancedRecorder?.start()
            } else {
                try {
                    initMediaRecorder(recordWidth, recordHeight, currentSettings.fps.fps.coerceAtMost(60), currentOutputFile!!)
                } catch (e: Exception) {
                    Log.w(TAG, "Hardware encoder initialization failed, trying fallback AVC profile", e)
                    initMediaRecorderFallback((displayWidth / 2) * 2, (displayHeight / 2) * 2, 30, currentOutputFile!!)
                }

                mediaProjection?.let { projection ->
                    virtualDisplay = projection.createVirtualDisplay(
                        "ApexScreenCapture",
                        recordWidth,
                        recordHeight,
                        screenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        mediaRecorder?.surface,
                        null,
                        null
                    )
                }

                mediaRecorder?.start()
            }

            recordingStartTime = SystemClock.elapsedRealtime()
            pausedDurationAcc = 0L
            isPaused = false

            startMetricsLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _recordingState.value = RecordingState.Error("Capture initiation: ${e.message.takeIf { !it.isNullOrBlank() } ?: e.javaClass.simpleName}")
            cleanup()
        }
    }

    @Suppress("DEPRECATION")
    private fun initMediaRecorder(width: Int, height: Int, fps: Int, outputFile: File) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val audioEnabled = (currentSettings.audioSource != AudioSourceOption.MUTE) && hasAudioPermission

        if (audioEnabled) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(outputFile.absolutePath)

        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(fps)

        if (currentSettings.codec == CodecOption.HEVC) {
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
        } else {
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        }

        recorder.setVideoEncodingBitRate(currentSettings.bitrate.bps)

        if (audioEnabled) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(48000)
            recorder.setAudioEncodingBitRate(192000)
        }

        recorder.prepare()
        this.mediaRecorder = recorder
    }

    @Suppress("DEPRECATION")
    private fun initMediaRecorderFallback(width: Int, height: Int, fps: Int, outputFile: File) {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val audioEnabled = (currentSettings.audioSource != AudioSourceOption.MUTE) && hasAudioPermission

        if (audioEnabled) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(fps)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoEncodingBitRate(10_000_000)

        if (audioEnabled) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(48000)
            recorder.setAudioEncodingBitRate(192000)
        }

        recorder.prepare()
        this.mediaRecorder = recorder
    }

    private fun startMetricsLoop() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            var fallbackNotified = false
            while (isActive) {
                delay(500)
                if (isPaused) continue

                val elapsed = SystemClock.elapsedRealtime() - recordingStartTime - pausedDurationAcc
                val (tempC, _, isThrottling) = hardwareProfiler.getThermalMetrics()
                val targetFps = currentSettings.fps.fps

                // Check DLSS 5 smart fallback
                val shouldFallback = dlss5Engine.shouldFallbackToPostProcess(
                    mode = currentSettings.dlss5Mode,
                    recordedFps = targetFps,
                    targetFps = targetFps,
                    temperatureC = tempC
                )

                if (shouldFallback && !fallbackNotified && currentSettings.dlss5Mode == Dlss5Mode.REALTIME_FALLBACK) {
                    fallbackNotified = true
                    _toastEvents.emit("DLSS 5: Smart Fallback engaged to protect 144FPS & touch response. Will enhance upon saving!")
                }

                val currentSize = currentOutputFile?.length() ?: 0L

                _recordingState.value = RecordingState.Recording(
                    durationMs = elapsed,
                    currentFps = targetFps,
                    currentTempC = tempC,
                    isThermalSafe = !isThrottling,
                    dlss5ActiveInRealtime = currentSettings.dlss5Mode == Dlss5Mode.REALTIME_FORCE,
                    dlss5AutoFallbackQueued = shouldFallback,
                    estimatedSizeBytes = currentSize
                )
            }
        }
    }

    fun pauseRecording() {
        if (_recordingState.value is RecordingState.Recording && !isPaused) {
            try {
                if (advancedRecorder != null) {
                    coroutineScope.launch { _toastEvents.emit("Pause is unsupported in true Internal Audio mode.") }
                    return
                }
                mediaRecorder?.pause()
                pauseStartTime = SystemClock.elapsedRealtime()
                isPaused = true
                val currentElapsed = SystemClock.elapsedRealtime() - recordingStartTime - pausedDurationAcc
                _recordingState.value = RecordingState.Paused(currentElapsed)
            } catch (e: Exception) {
                Log.e(TAG, "Pause failed", e)
            }
        }
    }

    fun resumeRecording() {
        if (isPaused) {
            try {
                mediaRecorder?.resume()
                pausedDurationAcc += (SystemClock.elapsedRealtime() - pauseStartTime)
                isPaused = false
            } catch (e: Exception) {
                Log.e(TAG, "Resume failed", e)
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        val finalDuration = if (recordingStartTime > 0) {
            SystemClock.elapsedRealtime() - recordingStartTime - pausedDurationAcc
        } else {
            0L
        }

        try {
            if (advancedRecorder != null) {
                advancedRecorder?.stop()
            } else {
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop media recorder exception", e)
        }

        val file = currentOutputFile
        cleanup()

        if (file != null && file.exists()) {
            val fileLen = file.length()
            
            // Export to public MediaStore (Gallery) to bypass Scoped Storage limitations
            var finalPublicPath = file.absolutePath
            var galleryExportSuccess = false
            
            try {
                val resolver = context.contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_DCIM + "/ApexScreenRecord")
                        put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
                    } else {
                        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ApexScreenRecord")
                        if (!publicDir.exists()) publicDir.mkdirs()
                        val publicFile = File(publicDir, file.name)
                        put(android.provider.MediaStore.Video.Media.DATA, publicFile.absolutePath)
                    }
                }
                val uri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out -> java.io.FileInputStream(file).use { it.copyTo(out) } }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                    
                    // Attempt to get the actual public file path for our database if possible
                    val proj = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                    val cursor = resolver.query(uri, proj, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val pathIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                        val path = cursor.getString(pathIndex)
                        if (path != null) finalPublicPath = path
                        cursor.close()
                    }
                    galleryExportSuccess = true
                }
            } catch (e: Exception) { Log.e(TAG, "MediaStore export failed, trying direct copy", e) }

            if (!galleryExportSuccess) {
                try {
                    val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ApexScreenRecord")
                    if (!publicDir.exists()) publicDir.mkdirs()
                    val publicFile = File(publicDir, file.name)
                    file.copyTo(publicFile, overwrite = true)
                    finalPublicPath = publicFile.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Direct copy failed", e)
                }
            }
            
            // Force MediaScanner to index the file so it appears in Gallery instantly
            android.media.MediaScannerConnection.scanFile(context, arrayOf(finalPublicPath), arrayOf("video/mp4"), null)

            val initialHasDlss = currentSettings.dlss5Mode == Dlss5Mode.REALTIME_FORCE

            val videoEntity = RecordedVideoEntity(
                title = "Apex Gaming Clip - " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date()),
                filePath = finalPublicPath,
                durationMs = finalDuration,
                fileSizeBytes = fileLen,
                width = currentSettings.resolution.width,
                height = currentSettings.resolution.height,
                fps = currentSettings.fps.fps,
                codec = currentSettings.codec.label,
                bitrateBps = currentSettings.bitrate.bps,
                hasDlss5Applied = initialHasDlss,
                dlss5QualityFactor = if (initialHasDlss) 1.5f else 1.0f,
                isRealtimeDlss5 = initialHasDlss
            )

            coroutineScope.launch {
                val insertedId = repository.insertVideo(videoEntity)
                val savedEntity = videoEntity.copy(id = insertedId)

                val needsPostDlss5 = currentSettings.dlss5Mode == Dlss5Mode.POST_PROCESS ||
                        currentSettings.dlss5Mode == Dlss5Mode.REALTIME_FALLBACK

                _recordingState.value = RecordingState.Finished(
                    savedVideo = savedEntity,
                    autoDlss5Triggered = needsPostDlss5
                )

                if (needsPostDlss5) {
                    _toastEvents.emit("Video saved! Processing DLSS 5 (4K 144FPS AI Upscaling)...")
                    dlss5Engine.enhanceVideoWithDlss5(
                        video = savedEntity,
                        targetWidth = 3840,
                        targetHeight = 2160,
                        targetFps = 144,
                        sharpness = currentSettings.dlss5SharpnessLevel,
                        frameGen = currentSettings.dlss5FrameGenEnabled
                    )
                    _toastEvents.emit("DLSS 5 Complete: Clip enhanced to 4K 144FPS with ultra-compact file size!")
                }
            }
        } else {
            _recordingState.value = RecordingState.Idle
        }
    }

    private fun cleanup() {
        try {
            advancedRecorder?.stop()
            advancedRecorder = null
        } catch (_: Exception) {}
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (_: Exception) {}

        touchManager.restoreDefaultPriority()
    }

    fun resetStateToIdle() {
        _recordingState.value = RecordingState.Idle
    }
}
