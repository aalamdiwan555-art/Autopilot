package com.mamabhutnika.rideaccepter

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow display over other apps before opening controls.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        try {
            windowManager.addView(floatingView, params)
        } catch (_: RuntimeException) {
            Toast.makeText(this, "Floating controls could not be opened.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val statusText = floatingView.findViewById<TextView>(R.id.floatingStatus)
        val btnStart = floatingView.findViewById<Button>(R.id.btnStart)
        val btnPause = floatingView.findViewById<Button>(R.id.btnPause)
        val btnStop = floatingView.findViewById<Button>(R.id.btnStop)
        val btnCancel = floatingView.findViewById<Button>(R.id.btnCancel)
        val prefs = UserPrefs(this)

        fun updateStatus() {
            statusText.text = when {
                !prefs.isEnabled -> "Mama Bhutnika: OFF"
                RideAccepterService.isPaused -> "Mama Bhutnika: PAUSED"
                else -> "Mama Bhutnika: RUNNING"
            }
        }

        btnStart.setOnClickListener {
            if (!prefs.hasActiveSubscription()) {
                Toast.makeText(this, "Your subscription is inactive.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.isEnabled = true
            RideAccepterService.isPaused = false
            updateStatus()
            Toast.makeText(this, "Auto-Click ENABLED", Toast.LENGTH_SHORT).show()
        }

        btnPause.setOnClickListener {
            RideAccepterService.isPaused = true
            updateStatus()
            Toast.makeText(this, "Auto-Click PAUSED", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            prefs.isEnabled = false
            RideAccepterService.isPaused = true
            updateStatus()
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
        }

        btnCancel.setOnClickListener {
            stopSelf()
        }

        updateStatus()

        // Drag functionality
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        statusText.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x
                    initialY = params!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params!!.x = initialX + (event.rawX - touchX).toInt()
                    params!!.y = initialY + (event.rawY - touchY).toInt()
            windowManager.updateViewLayout(floatingView, params!!)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            try {
                windowManager.removeView(floatingView)
            } catch (_: IllegalArgumentException) {
                // The window was already removed by the system.
            }
        }
    }
}
