package com.autopilot.driver

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: AppPrefs
    private lateinit var overlayStatus: TextView
    private lateinit var rewardedButton: Button
    private lateinit var rewardProgress: TextView
    private var rewardLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::overlayStatus.isInitialized) refreshPermissions()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildScreen(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(c(R.color.autopilot_background))
            setPadding(dp(18), dp(16), dp(18), 0)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val banner = TextView(this).apply {
            text = "ADVERTISEMENT"
            gravity = Gravity.CENTER
            textSize = 10f
            setTextColor(c(R.color.autopilot_muted))
        }
        content.addView(banner, LinearLayout.LayoutParams(-1, dp(52)))
        rewardedButton = Button(this).apply {
            text = "Watch an ad — earn a day"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setOnClickListener { watchRewardedAd() }
        }
        content.addView(rewardedButton, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(10), 0, dp(16)) })

        val subscription = card("SUBSCRIPTION")
        subscription.addView(text("Active until ${prefs.lastSubscription}", 17, R.color.autopilot_text))
        rewardProgress = text("", 13, R.color.autopilot_muted)
        subscription.addView(rewardProgress)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { progress = prefs.rewardCount * 10; max = 100 }
        subscription.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { setMargins(0, dp(10), 0, dp(4)) })
        content.addView(subscription)

        val setup = card("ONE-TIME SETUP")
        overlayStatus = text("", 14, R.color.autopilot_text)
        setup.addView(setupRow("Accessibility", RideAccessibilityService.isEnabled(this), Settings.ACTION_ACCESSIBILITY_SETTINGS))
        setup.addView(setupRow("Floating control", overlayAllowed(), null))
        setup.addView(setupRow("Screen capture", prefs.captureGranted, null))
        setup.addView(setupRow("Notification", Build.VERSION.SDK_INT < 33 || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0, null))
        setup.addView(setupRow("Internet", true, null))
        content.addView(setup)

        val supported = card("SUPPORTED APPS")
        supported.addView(text("Advanced matching is managed globally by admins.", 13, R.color.autopilot_muted))
        listOf("Rapido" to "rapido", "Ola" to "ola", "Uber" to "uber").forEach { (label, key) ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(text(label, 15, R.color.autopilot_text), LinearLayout.LayoutParams(0, dp(48), 1f))
            val toggle = Switch(this).apply {
                isChecked = prefs.appEnabled(key)
                setOnCheckedChangeListener { _, checked -> prefs.setAppEnabled(key, checked) }
            }
            row.addView(toggle)
            supported.addView(row)
        }
        content.addView(supported)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(10))
        }
        listOf("Home", "Refer & Earn", "Profile").forEach { label ->
            nav.addView(Button(this).apply { text = label; isAllCaps = false; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        root.addView(nav)
        refreshPermissions()
        return root
    }

    private fun setupRow(label: String, granted: Boolean, settingsAction: String?): LinearLayout =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(label, 14, R.color.autopilot_text), LinearLayout.LayoutParams(0, dp(44), 1f))
            addView(text(if (granted) "✓" else "Grant", 14, if (granted) R.color.autopilot_success else R.color.autopilot_primary).apply {
                if (settingsAction != null) setOnClickListener { startActivity(Intent(settingsAction)) }
            })
        }

    private fun refreshPermissions() {
        val overlay = overlayAllowed()
        if (::overlayStatus.isInitialized) overlayStatus.text = if (overlay) "Floating control enabled ✓" else "Floating control needs access"
        if (::overlayStatus.isInitialized) overlayStatus.setTextColor(c(if (overlay) R.color.autopilot_success else R.color.autopilot_muted))
        if (::rewardProgress.isInitialized) rewardProgress.text = "Reward ${prefs.rewardCount}/10"
    }

    private fun watchRewardedAd() {
        if (rewardLoading) return
        rewardLoading = true
        rewardedButton.isEnabled = false
        rewardedButton.text = "Loading reward…"
        AdManager.showRewardedAd(this, {
            prefs.rewardDays += 1
            prefs.rewardCount = (prefs.rewardCount + 1) % 10
            rewardLoading = false
            rewardedButton.isEnabled = true
            rewardedButton.text = "Watch an ad — earn a day"
            refreshPermissions()
            Toast.makeText(this, "Reward added for one day.", Toast.LENGTH_SHORT).show()
        }, {
            rewardLoading = false
            rewardedButton.isEnabled = true
            rewardedButton.text = "Watch an ad — earn a day"
        })
    }

    private fun overlayAllowed() = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)
    private fun card(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(12))
        setBackgroundColor(c(R.color.autopilot_card))
        addView(text(title, 11, R.color.autopilot_primary))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) }
    }
    private fun text(value: String, size: Int, color: Int) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(c(color)); setPadding(0, dp(4), 0, dp(4))
    }
    private fun c(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}