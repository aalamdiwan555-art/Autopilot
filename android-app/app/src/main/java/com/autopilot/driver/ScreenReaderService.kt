package com.autopilot.driver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScreenReaderService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "autopilot_running"
        private const val INTERVAL_MS = 1500L
        @Volatile var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val ocrInFlight = AtomicBoolean(false)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var prefs: AppPrefs
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var captureWidth = 0
    private var captureHeight = 0

    override fun onCreate() {
        super.onCreate()
        prefs = AppPrefs(this)
        isRunning = true
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Autopilot is running")
            .setContentText("Watching selected ride apps for new offers")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(41, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(41, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY
        if (projection == null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val width = screenWidth.coerceAtMost(1440)
        val height = screenHeight.coerceAtMost(2560)
        captureWidth = width
        captureHeight = height
        reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay(
            "AutopilotScreenReader", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
        )
        handler.post(captureLoop)
    }

    private val captureLoop = object : Runnable {
        override fun run() {
            if (prefs.autopilotEnabled &&
                RideAccessibilityService.foregroundPackage in setOf("com.rapido.rider", "com.olacabs.oladriver", "com.ubercab.driver", "com.ubercab")) {
                processLatestFrame()
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    private fun processLatestFrame() {
        if (!ocrInFlight.compareAndSet(false, true)) return
        val image = reader?.acquireLatestImage()
        if (image == null) {
            ocrInFlight.set(false)
            return
        }
        try {
            val bitmap = imageToBitmap(image)
            image.close()
            worker.execute {
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result ->
                        if (prefs.autopilotEnabled && OcrKeywords.containsAccept(result.text)) {
                            val targetBounds = findAcceptBounds(result)
                            if (targetBounds != null) {
                                RideAccessibilityService.requestAcceptClick(mapCaptureBoundsToScreen(targetBounds))
                            } else {
                                RideAccessibilityService.requestAcceptClick()
                            }
                        }
                    }
                    .addOnCompleteListener {
                        bitmap.recycle()
                        ocrInFlight.set(false)
                    }
            }
        } catch (_: Exception) {
            image.close()
            ocrInFlight.set(false)
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride
        val paddedBitmap = Bitmap.createBitmap(
            paddedWidth,
            image.height,
            Bitmap.Config.ARGB_8888,
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)

        if (paddedWidth == image.width) {
            return paddedBitmap
        }

        val croppedBitmap = Bitmap.createBitmap(
            paddedBitmap,
            0,
            0,
            image.width,
            image.height,
        )
        paddedBitmap.recycle()
        return croppedBitmap
    }

    private fun findAcceptBounds(result: Text): Rect? {
        val lines = result.textBlocks.flatMap { it.lines }
        return lines.firstOrNull { OcrKeywords.containsAccept(it.text) }?.boundingBox
            ?: result.textBlocks.firstOrNull { OcrKeywords.containsAccept(it.text) }?.boundingBox
    }

    private fun mapCaptureBoundsToScreen(bounds: Rect): Rect {
        if (captureWidth <= 0 || captureHeight <= 0 ||
            screenWidth <= 0 || screenHeight <= 0
        ) {
            return Rect(bounds)
        }
        val scaleX = screenWidth.toFloat() / captureWidth
        val scaleY = screenHeight.toFloat() / captureHeight
        return Rect(
            (bounds.left * scaleX).toInt(),
            (bounds.top * scaleY).toInt(),
            (bounds.right * scaleX).toInt(),
            (bounds.bottom * scaleY).toInt(),
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Autopilot status", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        ocrInFlight.set(false)
        display?.release()
        reader?.close()
        projection?.stop()
        recognizer.close()
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}