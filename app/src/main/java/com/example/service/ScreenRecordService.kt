package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.engine.ScreenRecorderManager

class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "apex_screen_record_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_REC"
        const val ACTION_STOP = "ACTION_STOP_REC"
        const val ACTION_PAUSE = "ACTION_PAUSE_REC"
        const val ACTION_RESUME = "ACTION_RESUME_REC"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
    }

    private var activeMediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification("Recording active (4K 144FPS DLSS 5)", "Zero touch latency active")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to startForeground", e)
                }

                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    try {
                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val projection = projectionManager.getMediaProjection(resultCode, resultData)
                        if (projection != null) {
                            activeMediaProjection = projection
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                projection.registerCallback(object : MediaProjection.Callback() {
                                    override fun onStop() {
                                        Log.d(TAG, "MediaProjection stopped by system")
                                        ScreenRecorderManager.instance?.stopRecording()
                                    }
                                }, null)
                            }
                            ScreenRecorderManager.instance?.startWithProjection(projection)
                        } else {
                            Log.e(TAG, "MediaProjection returned null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error acquiring MediaProjection", e)
                    }
                } else {
                    Log.w(TAG, "No valid resultCode or resultData provided to start recording")
                }
            }
            ACTION_STOP -> {
                ScreenRecorderManager.instance?.stopRecording()
                try {
                    activeMediaProjection?.stop()
                    activeMediaProjection = null
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            ACTION_PAUSE -> {
                ScreenRecorderManager.instance?.pauseRecording()
                updateNotification("Recording Paused", "Tap resume or stop from game bar")
            }
            ACTION_RESUME -> {
                ScreenRecorderManager.instance?.resumeRecording()
                updateNotification("Recording Resumed (144 FPS)", "Capturing gameplay smoothly")
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Apex Screen Recorder Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows screen recording status and quick controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & DLSS 5", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activeMediaProjection?.stop()
            activeMediaProjection = null
        } catch (_: Exception) {}
    }
}
