package com.mamabhutnika.rideaccepter

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * A deliberately guarded accessibility helper.
 *
 * It only considers exact positive labels in visible, enabled controls. A blank
 * package filter means every user app; the optional filter can narrow matching.
 */
class RideAccepterService : AccessibilityService() {

    companion object {
        const val TAG = "MamaBhutnika"
        var isRunning = false
        var isPaused = false
        var lastClickTime = 0L
        private const val CLICK_COOLDOWN = 120L
        private const val CLICK_DELAY = 25L

        const val RAPIDO_PACKAGE = "com.rapido.rider"
        const val OLA_PACKAGE = "com.olacabs.oladriver"
        const val UBER_PACKAGE = "com.ubercab.driver"
        private val BLOCKED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.mamabhutnika.rideaccepter",
        )

        private val ACCEPT_TEXTS = setOf(
            "Accept", "Accept Ride", "Accept Trip", "Accept Request", "Accept Order",
            "Accept & Go", "Tap to Accept", "Swipe to Accept", "Confirm Ride",
            "Confirm Booking", "Take Ride", "Get Ride", "Go Online", "Start Ride",
            "Accept Delivery", "Confirm Pickup",
            "स्वीकार करें", "स्वीकार", "स्वीकार करो", "स्वीकार करे", "स्वीकार कर",
            "स्वीकार करा", "मंजूर करें", "मंजूर", "मंजूर करे",
            "સ્વીકાર કરો", "સ્વીકારો", "માન્ય કરો", "મંજૂર કરો", "મંજૂર કરે",
            "গ্রহণ করুন", "গ্রহণ করো", "গ্রহণ", "স্বীকার করুন", "স্বীকার করো",
            "అంగీకరించండి", "స్వీకరించండి", "అంగీకరించు", "అంగీకరించండి",
            "ஏற்றுக்கொள்ளுங்கள்", "ஏற்கவும்", "அங்கீகரிக்கவும்",
            "قبول کریں", "قبول", "منظور",
            "ಸ್ವೀಕರಿಸಿ", "ಸ್ವೀಕರಿಸು", "ಒಪ್ಪಿಕೊಳ್ಳಿ",
            "ଗ୍ରହଣ କରନ୍ତୁ", "ସ୍ୱୀକାର କର",
            "സ്വീകരിക്കുക", "സ്വീകരിക്കൂ", "അംഗീകരിക്കുക",
            "ਸਵੀਕਾਰ ਕਰੋ", "ਮਨਜ਼ੂਰ", "ਮਨਜ਼ੂਰ ਕਰੋ",
            "मान्य करा", "स्वीकार गर्नुहोस्", "قبول ڪيو",
        )

        private val BLOCKED_LABELS = setOf(
            "cancel", "stop", "logout", "sign out", "close", "delete", "deny",
            "allow", "grant", "permission", "settings", "back",
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private var prefs: UserPrefs? = null
    private var customTexts = emptySet<String>()
    private var isEnabled = false
    private var targetPackage = ""
    private var clickScheduled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = UserPrefs(this)
        loadSettings()
        isRunning = prefs?.canUseAutoClicker() == true
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 25
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Toast.makeText(this, "Autopilot is ready", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        loadSettings()
        if (!isEnabled || isPaused || event == null || prefs?.canUseAutoClicker() != true) {
            isRunning = false
            return
        }

        val packageName = event.packageName?.toString() ?: ""
        if (packageName in BLOCKED_PACKAGES ||
            (targetPackage.isNotBlank() && packageName != targetPackage)
        ) {
            isRunning = isEnabled && prefs?.canUseAutoClicker() == true
            return
        }

        isRunning = true
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) return

        val root = rootInActiveWindow ?: return
        try {
            scanAndClick(root, packageName)
        } catch (error: Exception) {
            Log.w(TAG, "Could not inspect the ride offer window", error)
        }
    }

    private fun scanAndClick(root: AccessibilityNodeInfo, packageName: String) {
        if (clickScheduled || System.currentTimeMillis() - lastClickTime < CLICK_COOLDOWN) return
        val labels = ACCEPT_TEXTS + customTexts

        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (candidate in nodes) {
                try {
                    if (isSafeCandidate(candidate, label)) {
                        scheduleClick(label, packageName)
                        return
                    }
                } finally {
                    candidate.recycle()
                }
            }
        }
    }

    private fun scheduleClick(label: String, packageName: String) {
        clickScheduled = true
        handler.postDelayed({
            try {
                val root = rootInActiveWindow
                val activePackage = root?.packageName?.toString()
                if (!isEnabled || isPaused || prefs?.canUseAutoClicker() != true ||
                    root == null || activePackage != packageName ||
                    System.currentTimeMillis() - lastClickTime < CLICK_COOLDOWN
                ) return@postDelayed

                // Re-query after the delay. Ride offer cards are frequently
                // recreated by Compose/RecyclerView and the original node can
                // become stale before the click is dispatched.
                val freshNode = root.findAccessibilityNodeInfosByText(label)
                    ?.firstOrNull { isSafeCandidate(it, label) }
                if (freshNode != null && performClick(freshNode)) {
                    lastClickTime = System.currentTimeMillis()
                    Log.i(TAG, "Accepted guarded ride control '$label' in $packageName")
                    Toast.makeText(this, "Ride offer accepted", Toast.LENGTH_SHORT).show()
                }
                if (freshNode != null) {
                    freshNode.recycle()
                }
            } finally {
                clickScheduled = false
            }
        }, CLICK_DELAY)
    }

    private fun isSafeCandidate(node: AccessibilityNodeInfo, label: String): Boolean {
        val text = node.text?.toString()?.trim().orEmpty()
        val description = node.contentDescription?.toString()?.trim().orEmpty()
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim().orEmpty()
        } else {
            ""
        }
        val wanted = normalize(label)
        val exactMatch = listOf(text, description, hint)
            .map(::normalize)
            .any { value -> value == wanted || value.contains(wanted) }
        val blocked = listOf(text, description, hint).any { value ->
            BLOCKED_LABELS.any { blockedLabel ->
                value.equals(blockedLabel, ignoreCase = true)
            }
        }
        return exactMatch && !blocked && node.isEnabled && node.isVisibleToUser &&
            (node.isClickable || hasClickableParent(node) || hasVisibleBounds(node))
    }

    private fun normalize(value: String): String {
        return value.trim().replace(Regex("\\s+"), " ").lowercase()
    }

    private fun hasClickableParent(node: AccessibilityNodeInfo): Boolean {
        var parent = node.parent
        repeat(4) {
            if (parent == null) return false
            if (parent.isClickable && parent.isEnabled && parent.isVisibleToUser) {
                parent.recycle()
                return true
            }
            val next = parent.parent
            parent.recycle()
            parent = next
        }
        parent?.recycle()
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        // Some apps expose ACTION_CLICK without setting isClickable.
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.isEnabled &&
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                parent.recycle()
                return true
            }
            val next = parent.parent
            parent.recycle()
            parent = next
        }
        return dispatchTap(node)
    }

    private fun hasVisibleBounds(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return node.isEnabled && node.isVisibleToUser && bounds.width() > 0 && bounds.height() > 0
    }

    private fun dispatchTap(node: AccessibilityNodeInfo): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !hasVisibleBounds(node)) return false
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 1f, y + 1f)
        }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
                .build(),
            null,
            null,
        )
    }

    private fun loadSettings() {
        prefs?.let { current ->
            isEnabled = current.isEnabled
            targetPackage = current.targetPackage.trim()
            customTexts = current.customTexts.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }

    override fun onInterrupt() {
        clickScheduled = false
        isRunning = false
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        super.onDestroy()
    }
}