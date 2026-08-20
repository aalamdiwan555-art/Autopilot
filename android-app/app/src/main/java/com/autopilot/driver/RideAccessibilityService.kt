package com.autopilot.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RideAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "RideAccessibility"
        private var instance: RideAccessibilityService? = null
        private const val CLICK_COOLDOWN_MS = 900L
        @Volatile var foregroundPackage: String = ""
        private val ridePackages = setOf(
            "com.rapido.rider", "com.olacabs.oladriver", "com.ubercab.driver",
            "com.ubercab", "com.ubercab.eats"
        )
        fun isEnabled(context: android.content.Context): Boolean =
            (context.getSystemService(ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager)
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true
        fun requestAcceptClick() { instance?.requestClick(null) }
        fun requestAcceptClick(bounds: Rect) {
            instance?.requestClick(Rect(bounds))
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastClickAt = 0L
    private var gestureInFlight = false

    override fun onServiceConnected() {
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        foregroundPackage = event?.packageName?.toString().orEmpty()
    }

    private fun requestClick(bounds: Rect?) {
        if (!AppPrefs(this).autopilotEnabled) return
        mainHandler.post {
            // OCR gives the best result for canvas-rendered driver apps. If
            // Android rejects the gesture, use the accessibility tree instead.
            if (bounds == null || !clickAt(bounds)) clickAccept()
        }
    }

    private fun clickAt(bounds: Rect): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickAt < CLICK_COOLDOWN_MS || gestureInFlight) {
            Log.d(TAG, "Gesture skipped: cooldown or another gesture is active")
            return false
        }
        if (foregroundPackage !in ridePackages) {
            Log.d(TAG, "Gesture skipped: unsupported foreground package=$foregroundPackage")
            return false
        }
        if (bounds.isEmpty) {
            Log.d(TAG, "Gesture skipped: empty bounds=$bounds")
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.d(TAG, "Gesture unavailable before API 24; using node fallback")
            return false
        }

        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        gestureInFlight = true
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    lastClickAt = System.currentTimeMillis()
                    Log.i(TAG, "Screen gesture completed at (${bounds.exactCenterX()}, ${bounds.exactCenterY()})")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    Log.w(TAG, "Screen gesture cancelled; trying accessibility-node fallback")
                    mainHandler.post { clickAccept() }
                }
            },
            mainHandler,
        )
        if (accepted) {
            Log.d(TAG, "Screen gesture accepted for dispatch at $bounds")
            return true
        }
        gestureInFlight = false
        Log.w(TAG, "Screen gesture rejected by AccessibilityService")
        return false
    }

    private fun clickAccept(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickAt < CLICK_COOLDOWN_MS || gestureInFlight) {
            Log.d(TAG, "Node click skipped: cooldown or gesture is active")
            return false
        }
        if (foregroundPackage !in ridePackages) {
            Log.d(TAG, "Node click skipped: unsupported foreground package=$foregroundPackage")
            return false
        }
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "Node click skipped: active accessibility window is unavailable")
            return false
        }
        val candidate = findCandidate(root)
        if (candidate != null) {
            val clicked = performClick(candidate)
            if (clicked && !gestureInFlight) {
                lastClickAt = System.currentTimeMillis()
            }
            Log.i(TAG, "Node click result=$clicked")
            candidate.recycle()
            return clicked
        } else {
            Log.d(TAG, "Node click found no accept candidate")
            return false
        }
    }

    private fun findCandidate(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = listOf(node.text, node.contentDescription)
            .filterNotNull().joinToString(" ")
        if (OcrKeywords.containsAccept(label) && node.isVisibleToUser && node.isEnabled) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = try { findCandidate(child) } finally { child.recycle() }
            if (result != null) return result
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var parent = node.parent
        repeat(4) {
            if (parent == null) return@repeat
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                parent.recycle()
                return true
            }
            val next = parent.parent
            parent.recycle()
            parent = next
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false
        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
            lineTo(bounds.exactCenterX() + 1f, bounds.exactCenterY() + 1f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        gestureInFlight = true
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    lastClickAt = System.currentTimeMillis()
                    Log.i(TAG, "Node bounds gesture completed")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    Log.w(TAG, "Node bounds gesture cancelled")
                }
            },
            mainHandler,
        )
        if (!accepted) gestureInFlight = false
        return accepted
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        gestureInFlight = false
        if (instance === this) instance = null
        foregroundPackage = ""
        super.onDestroy()
    }
}
