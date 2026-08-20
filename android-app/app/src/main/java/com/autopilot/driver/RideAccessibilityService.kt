package com.autopilot.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RideAccessibilityService : AccessibilityService() {
    companion object {
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
        mainHandler.post { if (bounds != null) clickAt(bounds) else clickAccept() }
    }

    private fun clickAt(bounds: Rect) {
        if (System.currentTimeMillis() - lastClickAt < CLICK_COOLDOWN_MS) return
        if (foregroundPackage !in ridePackages || bounds.isEmpty) return
        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80))
                    .build(),
                null,
                null,
            )
        ) {
            lastClickAt = System.currentTimeMillis()
        }
    }

    private fun clickAccept() {
        if (System.currentTimeMillis() - lastClickAt < CLICK_COOLDOWN_MS) return
        if (foregroundPackage !in ridePackages) return
        val root = rootInActiveWindow ?: return
        val candidate = findCandidate(root)
        if (candidate != null) {
            if (performClick(candidate)) lastClickAt = System.currentTimeMillis()
            candidate.recycle()
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
        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        return dispatchGesture(
            android.accessibilityservice.GestureDescription.Builder()
                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80))
                .build(), null, null
        )
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        instance = null
        foregroundPackage = ""
        super.onDestroy()
    }
}