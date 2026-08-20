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
import kotlin.math.roundToInt

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
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(41, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else
            startForeground(41, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY
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
                RideAccessibilityService.foregroundPackage in setOf(
                    "com.rapido.rider", "com.olacabs.oladriver",
                    "com.ubercab.driver", "com.ubercab"
                )
            ) {
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
                                val screenBounds = mapCaptureBoundsToScreen(targetBounds)
                                RideAccessibilityService.requestAcceptClick(screenBounds)
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
            paddedWidth, image.height, Bitmap.Config.ARGB_8888
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)

        return if (paddedWidth == image.width) {
            paddedBitmap
        } else {
            val croppedBitmap = Bitmap.createBitmap(
                paddedBitmap, 0, 0, image.width, image.height
            )
            paddedBitmap.recycle()
            croppedBitmap
        }
    }

    /**
     * FIXED: Instead of returning the first match, collect every match and
     * pick the *smallest* bounding box. A single "Accept" line is more
     * precise than a massive text block that happens to contain the word.
     */
    private fun findAcceptBounds(result: Text): Rect? {
        val matches = mutableListOf<Rect>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                if (OcrKeywords.containsAccept(line.text)) {
                    line.boundingBox?.let { matches.add(it) }
                }
            }
            if (OcrKeywords.containsAccept(block.text)) {
                block.boundingBox?.let { matches.add(it) }
            }
        }
        // Smallest area = most precise target
        return matches.minByOrNull { it.width() * it.height() }
    }

    /**
     * FIXED: Use roundToInt() instead of toInt() so we don't drift by a pixel.
     * Also clamp the result so it can never be negative or larger than the screen.
     */
    private fun mapCaptureBoundsToScreen(bounds: Rect): Rect {
        if (captureWidth <= 0 || captureHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) {
            return Rect(bounds)
        }
        val scaleX = screenWidth.toFloat() / captureWidth
        val scaleY = screenHeight.toFloat() / captureHeight

        val left   = (bounds.left   * scaleX).roundToInt().coerceIn(0, screenWidth)
        val top    = (bounds.top    * scaleY).roundToInt().coerceIn(0, screenHeight)
        val right  = (bounds.right  * scaleX).roundToInt().coerceIn(0, screenWidth)
        val bottom = (bounds.bottom * scaleY).roundToInt().coerceIn(0, screenHeight)

        return Rect(left, top, right, bottom)
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
