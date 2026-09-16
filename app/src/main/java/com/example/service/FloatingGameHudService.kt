package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.engine.RecordingState
import com.example.engine.ScreenRecorderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingGameHudService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var overlayFilterView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Check if DLSS5 Real-Time Overlay is enabled in settings
            val settings = ScreenRecorderManager.instance?.currentSettings
            if (settings?.dlss5RealTimeOverlay == true) {
                setupScreenFilterOverlay()
            }
            
            setupFloatingWidget()
            observeRecordingState()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun setupScreenFilterOverlay() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Apply a subtle contrast/vibrancy filter using a translucent colored view
        overlayFilterView = View(this).apply {
            setBackgroundColor(0x1500E5FF) // Slight Cyber Cyan tint for enhanced vibrancy
        }

        try {
            windowManager?.addView(overlayFilterView, overlayParams)
        } catch (_: Exception) {}
    }

    private fun setupFloatingWidget() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        // Create sleek programmatically styled HUD container
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(0xEE111827.toInt()) // Sleek dark slate
            elevation = 16f
        }

        val recIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.presence_online)
            setColorFilter(0xFFFF1744.toInt()) // Red glowing dot
        }

        val textStats = TextView(this).apply {
            text = " 144 FPS | 4K DLSS 5"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            setPadding(12, 0, 16, 0)
        }

        val actionBtn = TextView(this).apply {
            text = "STOP"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 12f
            setPadding(16, 8, 16, 8)
            setBackgroundColor(0x3300E5FF.toInt())
            setOnClickListener {
                ScreenRecorderManager.instance?.stopRecording()
            }
        }

        container.addView(recIcon)
        container.addView(textStats)
        container.addView(actionBtn)

        // Draggable touch listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(container, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                            // Clicked: open main activity
                            val appIntent = Intent(this@FloatingGameHudService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(appIntent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        this.floatingView = container
        try {
            windowManager?.addView(container, params)
        } catch (_: Exception) {}
    }

    private fun observeRecordingState() {
        serviceScope.launch {
            ScreenRecorderManager.instance?.recordingState?.collectLatest { state ->
                when (state) {
                    is RecordingState.Idle, is RecordingState.Finished, is RecordingState.Error -> {
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayFilterView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
    }
}
