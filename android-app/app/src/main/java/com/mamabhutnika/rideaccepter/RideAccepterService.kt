package com.mamabhutnika.rideaccepter

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
 * It only considers exact positive ride-offer labels inside an explicitly selected
 * supported driver app. It must never be used as a general-purpose clicker.
 */
class RideAccepterService : AccessibilityService() {

    companion object {
        const val TAG = "MamaBhutnika"
        var isRunning = false
        var isPaused = false
        var lastClickTime = 0L
        private const val CLICK_COOLDOWN = 750L
        private const val CLICK_DELAY = 35L

        const val RAPIDO_PACKAGE = "com.rapido.rider"
        const val OLA_PACKAGE = "com.olacabs.oladriver"
        const val UBER_PACKAGE = "com.ubercab.driver"
        private val ALLOWED_PACKAGES = setOf(RAPIDO_PACKAGE, OLA_PACKAGE, UBER_PACKAGE)

        private val ACCEPT_TEXTS = setOf(
            "Accept", "Accept Ride", "Accept Trip", "Accept Request", "Accept Order",
            "Accept & Go", "Tap to Accept", "Swipe to Accept", "Confirm Ride",
            "Confirm Booking", "Take Ride", "Get Ride", "Go Online", "Start Ride",
            "Accept Delivery", "Confirm Pickup",
            "स्वीकार करें", "स्वीकार", "स्वीकार करो", "मंजूर करें", "मंजूर",
            "સ્વીકાર કરો", "સ્વીકારો", "માન્ય કર",
            "গ্রহণ করুন", "স্বীকার করুন", "গ্রহণ",
            "అంగీకరించండి", "స్వీకరించండి", "అంగీకరించు",
            "ஏற்றுக்கொள்ளுங்கள்", "ஏற்கவும்",
            "قبول کریں", "قبول", "منظور",
            "ಸ್ವೀಕರಿಸಿ", "ಒಪ್ಪಿಕೊಳ್ಳಿ",
            "ଗ୍ରହଣ କରନ୍ତୁ", "ସ୍ୱୀକାର କର",
            "സ്വീകരിക്കുക", "അംഗീകരിക്കുക",
            "ਸਵੀਕਾਰ ਕਰੋ", "ਮਨਜ਼ੂਰ",
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
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 25
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Toast.makeText(this, "Mama Bhutnika is ready", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        loadSettings()
        if (!isEnabled || isPaused || event == null || prefs?.canUseAutoClicker() != true) {
            isRunning = false
            return
        }

        val packageName = event.packageName?.toString() ?: ""
        if (packageName !in ALLOWED_PACKAGES ||
            (targetPackage.isNotBlank() && packageName != targetPackage)
        ) {
            isRunning = false
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
                        scheduleClick(AccessibilityNodeInfo.obtain(candidate), label, packageName)
                        return
                    }
                } finally {
                    candidate.recycle()
                }
            }
        }
    }

    private fun scheduleClick(node: AccessibilityNodeInfo, label: String, packageName: String) {
        clickScheduled = true
        handler.postDelayed({
            try {
                val activePackage = rootInActiveWindow?.packageName?.toString()
                if (!isEnabled || isPaused || prefs?.canUseAutoClicker() != true ||
                    activePackage != packageName ||
                    System.currentTimeMillis() - lastClickTime < CLICK_COOLDOWN
                ) return@postDelayed

                if (performClick(node)) {
                    lastClickTime = System.currentTimeMillis()
                    Log.i(TAG, "Accepted guarded ride control '$label' in $packageName")
                    Toast.makeText(this, "Ride offer accepted", Toast.LENGTH_SHORT).show()
                }
            } finally {
                clickScheduled = false
                node.recycle()
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
        val exactMatch = text.equals(label.trim(), ignoreCase = true) ||
            description.equals(label.trim(), ignoreCase = true) ||
            hint.equals(label.trim(), ignoreCase = true)
        val blocked = listOf(text, description, hint).any { value ->
            BLOCKED_LABELS.any { blockedLabel ->
                value.equals(blockedLabel, ignoreCase = true)
            }
        }
        return exactMatch && !blocked && node.isEnabled && node.isVisibleToUser &&
            (node.isClickable || hasClickableParent(node))
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
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
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
        return false
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