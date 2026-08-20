package com.autopilot.driver

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: AppPrefs
    private lateinit var statusTitle: TextView
    private lateinit var statusCopy: TextView
    private lateinit var startButton: Button
    private lateinit var rewardButton: Button
    private lateinit var topAd: LinearLayout
    private lateinit var statusAd: LinearLayout
    private lateinit var setupAd: LinearLayout
    private lateinit var rewardAd: LinearLayout
    private lateinit var bottomAd: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var permissionsCard: LinearLayout
    private lateinit var rewardsCard: LinearLayout
    private val permissionStatus = mutableMapOf<String, TextView>()
    private var rewardLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        setContentView(buildScreen())
        AdManager.attachBanner(topAd)
        AdManager.attachBanner(statusAd)
        AdManager.attachBanner(setupAd)
        AdManager.attachBanner(rewardAd)
        AdManager.attachBanner(bottomAd)
        animateEntrance()
    }

    override fun onResume() {
        super.onResume()
        if (::startButton.isInitialized) refreshState()
    }

    override fun onDestroy() {
        startButton.clearAnimation()
        super.onDestroy()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(c(R.color.autopilot_background))
            setPadding(dp(18), dp(14), dp(18), 0)
        }
        topAd = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(topAd, LinearLayout.LayoutParams(-1, dp(56)))

        scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(12))
        }
        content.addView(label("AUTOPILOT", 12, R.color.autopilot_primary))
        content.addView(title("Your ride desk", 32))
        content.addView(label("One tap to start. Autopilot stays quiet until you turn it on.", 15, R.color.autopilot_muted))

        val statusCard = card()
        statusTitle = title("Ready when you are", 22)
        statusCopy = label("", 14, R.color.autopilot_muted)
        statusCard.addView(statusTitle)
        statusCard.addView(statusCopy)
        startButton = Button(this).apply {
            text = "START AUTOPILOT"
            isAllCaps = false
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(c(R.color.ink_950))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_primary)
            stateListAnimator = null
            setOnClickListener { toggleAutopilot() }
        }
        statusCard.addView(startButton, LinearLayout.LayoutParams(-1, dp(58)).apply {
            setMargins(0, dp(16), 0, 0)
        })
        content.addView(statusCard)
        statusAd = adSlot()
        content.addView(statusAd)

        permissionsCard = card()
        permissionsCard.addView(label("SETUP", 11, R.color.autopilot_primary))
        permissionsCard.addView(label("Permissions are remembered. Tap a missing item only when Android asks you to review it.", 13, R.color.autopilot_muted))
        permissionRow(permissionsCard, "Accessibility", { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { RideAccessibilityService.isEnabled(this) }
        permissionRow(permissionsCard, "Floating control", {
            if (Build.VERSION.SDK_INT >= 23) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }) { overlayAllowed() }
        permissionRow(permissionsCard, "Screen capture", { requestScreenCapture() }) { prefs.captureGranted }
        permissionRow(permissionsCard, "Notifications", { requestNotifications() }) { notificationGranted() }
        content.addView(permissionsCard)
        setupAd = adSlot()
        content.addView(setupAd)

        rewardsCard = card()
        rewardsCard.addView(label("OPTIONAL REWARDS", 11, R.color.autopilot_primary))
        rewardsCard.addView(label("Watch a real Start.io rewarded video to earn extra time.", 13, R.color.autopilot_muted))
        rewardButton = Button(this).apply {
            text = "Watch a rewarded ad"
            isAllCaps = false
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_secondary)
            setTextColor(c(R.color.autopilot_text))
            setOnClickListener { watchRewardedAd() }
        }
        rewardsCard.addView(rewardButton, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(10), 0, 0) })
        content.addView(rewardsCard)
        rewardAd = adSlot()
        content.addView(rewardAd)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        bottomAd = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(bottomAd, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(buildNavigation(), LinearLayout.LayoutParams(-1, dp(64)))
        return root
    }

    private fun adSlot() = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        contentDescription = "Advertisement"
        layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply {
            setMargins(0, dp(2), 0, dp(8))
        }
    }

    private fun buildNavigation() = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        setPadding(0, dp(2), 0, dp(8))
        addView(navItem("Home", android.R.drawable.ic_menu_view) { scroll.fullScroll(View.FOCUS_UP) }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(navItem("Rewards", android.R.drawable.ic_menu_agenda) { scroll.smoothScrollTo(0, rewardsCard.top) }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(navItem("Settings", android.R.drawable.ic_menu_preferences) { scroll.smoothScrollTo(0, permissionsCard.top) }, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun navItem(text: String, icon: Int, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_nav_item)
        contentDescription = "$text section"
        setOnClickListener { action() }
        addView(ImageView(this@MainActivity).apply {
            setImageResource(icon)
            setColorFilter(c(R.color.autopilot_primary))
            contentDescription = "$text logo"
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
        addView(label(text, 11, R.color.autopilot_muted))
    }

    private fun permissionRow(parent: LinearLayout, label: String, action: () -> Unit, check: () -> Boolean) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(4))
            isClickable = true
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_permission)
        }
        row.addView(label(label, 14, R.color.autopilot_text), LinearLayout.LayoutParams(0, dp(42), 1f))
        val status = label(if (check()) "READY" else "REVIEW", 11, if (check()) R.color.autopilot_success else R.color.autopilot_primary).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = .08f
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }
        row.setOnClickListener { if (!check()) action() }
        permissionStatus[label] = status
        row.addView(status)
        parent.addView(row)
    }

    private fun refreshState() {
        val accessibility = RideAccessibilityService.isEnabled(this)
        val capture = prefs.captureGranted
        val ready = accessibility && capture && overlayAllowed()
        val running = prefs.autopilotEnabled && ScreenReaderService.isRunning
        statusTitle.text = if (running) "Autopilot is running" else "Ready when you are"
        statusCopy.text = when {
            running -> "Watching for visible ride offers. You stay in control."
            !ready -> "Complete the highlighted setup items before starting."
            else -> "Your permissions are ready. Start when you are ready to drive."
        }
        startButton.text = if (running) "PAUSE AUTOPILOT" else "START AUTOPILOT"
        startButton.background = ContextCompat.getDrawable(
            this,
            if (running) R.drawable.bg_button_secondary else R.drawable.bg_button_primary,
        )
        startButton.setTextColor(c(if (running) R.color.autopilot_text else R.color.ink_950))
        updatePermission("Accessibility", accessibility)
        updatePermission("Floating control", overlayAllowed())
        updatePermission("Screen capture", capture)
        updatePermission("Notifications", notificationGranted())
        if (running) startPulse() else startButton.clearAnimation()
    }

    private fun toggleAutopilot() {
        if (prefs.autopilotEnabled && ScreenReaderService.isRunning) {
            prefs.autopilotEnabled = false
            stopService(Intent(this, ScreenReaderService::class.java))
            refreshState()
            Toast.makeText(this, "Autopilot paused.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!RideAccessibilityService.isEnabled(this) || !overlayAllowed() || !prefs.captureGranted) {
            Toast.makeText(this, "Review the highlighted setup items first.", Toast.LENGTH_LONG).show()
            return
        }
        prefs.autopilotEnabled = true
        if (!ScreenReaderService.isRunning) requestScreenCapture()
        refreshState()
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_REQUEST)
    }

    @Deprecated("Android activity result API retained for API 24 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAPTURE_REQUEST || resultCode != RESULT_OK || data == null) {
            prefs.autopilotEnabled = false
            refreshState()
            return
        }
        prefs.captureGranted = true
        val intent = Intent(this, ScreenReaderService::class.java).apply {
            putExtra(ScreenReaderService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenReaderService.EXTRA_RESULT_DATA, data)
        }
        ContextCompat.startForegroundService(this, intent)
        refreshState()
    }

    private fun watchRewardedAd() {
        if (rewardLoading) return
        rewardLoading = true
        rewardButton.isEnabled = false
        rewardButton.text = "Loading real ad…"
        AdManager.showRewardedAd(this, {
            prefs.rewardDays += 1
            rewardLoading = false
            rewardButton.isEnabled = true
            rewardButton.text = "Watch a rewarded ad"
            Toast.makeText(this, "Reward added for one day.", Toast.LENGTH_SHORT).show()
        }, {
            rewardLoading = false
            rewardButton.isEnabled = true
            rewardButton.text = "Watch a rewarded ad"
        })
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 11)
    }

    private fun notificationGranted() =
        Build.VERSION.SDK_INT < 33 || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0

    private fun updatePermission(name: String, granted: Boolean) {
        permissionStatus[name]?.apply {
            text = if (granted) "READY" else "REVIEW"
            setTextColor(c(if (granted) R.color.autopilot_success else R.color.autopilot_primary))
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(450L).start()
    }

    private fun startPulse() {
        if (startButton.animation != null) return
        startButton.animate().scaleX(1.03f).scaleY(1.03f).setDuration(700L)
            .withEndAction {
                startButton.animate().scaleX(1f).scaleY(1f).setDuration(700L)
                    .withEndAction { if (prefs.autopilotEnabled) startPulse() }.start()
            }.start()
    }

    private fun overlayAllowed() = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(12))
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) }
    }
    private fun label(value: String, size: Int, color: Int) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(c(color))
        setPadding(0, dp(4), 0, dp(4))
        includeFontPadding = false
    }
    private fun title(value: String, size: Int) = label(value, size, R.color.autopilot_text).apply {
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun c(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CAPTURE_REQUEST = 701
    }
}