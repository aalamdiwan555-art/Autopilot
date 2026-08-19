package com.mamabhutnika.rideaccepter

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Secure
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val RAPIDO_PACKAGE = "com.rapido.rider"
        const val OLA_PACKAGE = "com.olacabs.oladriver"
        const val UBER_PACKAGE = "com.ubercab.driver"
    }

    private lateinit var statusText: TextView
    private lateinit var tvAdStatus: TextView
    private lateinit var tvRewardProgress: TextView
    private lateinit var toggleSwitch: SwitchCompat
    private lateinit var rapidoModeSwitch: SwitchCompat
    private lateinit var olaModeSwitch: SwitchCompat
    private lateinit var uberModeSwitch: SwitchCompat
    private lateinit var openSettingsBtn: Button
    private lateinit var customTextInput: EditText
    private lateinit var targetPackageInput: EditText
    private lateinit var saveBtn: Button
    private lateinit var floatingBtn: Button
    private lateinit var overlayBtn: Button
    private lateinit var btnWatchAd: Button
    private lateinit var tvReferralCode: TextView
    private lateinit var etReferralCode: EditText
    private lateinit var btnRedeemReferral: Button
    private lateinit var btnShareReferral: Button
    private lateinit var btnLogout: Button
    private lateinit var bannerContainer: LinearLayout

    private lateinit var prefs: UserPrefs
    private lateinit var adManager: AdManager
    private val api = ApiClient()
    private var isUpdatingAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = UserPrefs(this)
        adManager = AdManager.getInstance(this)

        statusText = findViewById(R.id.statusText)
        tvAdStatus = findViewById(R.id.tvAdStatus)
        tvRewardProgress = findViewById(R.id.tvRewardProgress)
        toggleSwitch = findViewById(R.id.toggleSwitch)
        rapidoModeSwitch = findViewById(R.id.rapidoModeSwitch)
        olaModeSwitch = findViewById(R.id.olaModeSwitch)
        uberModeSwitch = findViewById(R.id.uberModeSwitch)
        openSettingsBtn = findViewById(R.id.openSettingsBtn)
        customTextInput = findViewById(R.id.customTextInput)
        targetPackageInput = findViewById(R.id.targetPackageInput)
        saveBtn = findViewById(R.id.saveBtn)
        floatingBtn = findViewById(R.id.floatingBtn)
        overlayBtn = findViewById(R.id.overlayBtn)
        btnWatchAd = findViewById(R.id.btnWatchAd)
        tvReferralCode = findViewById(R.id.tvReferralCode)
        etReferralCode = findViewById(R.id.etReferralCode)
        btnRedeemReferral = findViewById(R.id.btnRedeemReferral)
        btnShareReferral = findViewById(R.id.btnShareReferral)
        btnLogout = findViewById(R.id.btnLogout)
        bannerContainer = findViewById(R.id.bannerContainer)
        startStatusAnimation()

        // --- Banner Ad (bottom of dashboard) ---
        adManager.attachBanner(bannerContainer)

        // --- Main toggle ---
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAccount) return@setOnCheckedChangeListener
            if (isChecked && !prefs.hasActiveSubscription()) {
                isUpdatingAccount = true
                toggleSwitch.isChecked = false
                isUpdatingAccount = false
                Toast.makeText(
                    this,
                    "Subscription expired. Watch ads to renew before enabling auto-click.",
                    Toast.LENGTH_LONG,
                ).show()
                return@setOnCheckedChangeListener
            }
            prefs.isEnabled = isChecked
            updateStatus()
        }

        // --- Mode switches (mutually exclusive) ---
        rapidoModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAccount) return@setOnCheckedChangeListener
            if (isChecked) {
                olaModeSwitch.isChecked = false
                uberModeSwitch.isChecked = false
            }
            setTargetPackage(if (isChecked) RAPIDO_PACKAGE else "")
        }

        olaModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAccount) return@setOnCheckedChangeListener
            if (isChecked) {
                rapidoModeSwitch.isChecked = false
                uberModeSwitch.isChecked = false
            }
            setTargetPackage(if (isChecked) OLA_PACKAGE else "")
        }

        uberModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAccount) return@setOnCheckedChangeListener
            if (isChecked) {
                rapidoModeSwitch.isChecked = false
                olaModeSwitch.isChecked = false
            }
            setTargetPackage(if (isChecked) UBER_PACKAGE else "")
        }

        // --- Action buttons with interstitials ---
        openSettingsBtn.setOnClickListener {
            openAccessibilitySettings()
        }

        saveBtn.setOnClickListener {
            prefs.customTexts = customTextInput.text.toString()
            prefs.targetPackage = targetPackageInput.text.toString().trim()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }

        floatingBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                startService(Intent(this, FloatingWindowService::class.java))
                Toast.makeText(this, "Floating window started", Toast.LENGTH_SHORT).show()
            }
        }

        overlayBtn.setOnClickListener {
            requestOverlayPermission()
        }

        // --- Rewarded Video ---
        btnWatchAd.setOnClickListener {
            adManager.showRewardedVideo(this,
                onRewarded = { updateRewardUI() },
                onFailed = { updateRewardUI() }
            )
        }

        btnRedeemReferral.setOnClickListener {
            val code = etReferralCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter a referral code first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            redeemReferral(code)
        }

        btnShareReferral.setOnClickListener {
            val code = prefs.referralCode
            if (code.isBlank()) {
                Toast.makeText(this, "Your referral code is still loading.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Download Autopilot and enter my referral code $code to give me 2 free subscription days.",
                        )
                    },
                    "Share referral code",
                ),
            )
        }

        btnLogout.setOnClickListener {
            RideAccepterService.isPaused = true
            prefs.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadSettings()
        updateStatus()
        updateRewardUI()
        updateAdStatus()
        updateReferralUI()

        showFirstRunStart()
    }

    private fun setTargetPackage(pkg: String) {
        targetPackageInput.setText(pkg)
        prefs.targetPackage = pkg
        prefs.isRapidoMode = rapidoModeSwitch.isChecked
        prefs.isOlaMode = olaModeSwitch.isChecked
        prefs.isUberMode = uberModeSwitch.isChecked
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccount()
        updateStatus()
        updateRewardUI()
        updateAdStatus()
        // Re-attach banner in case ad state changed
        adManager.attachBanner(bannerContainer)
        maybeRequestOverlayPermission()
    }

    private fun showFirstRunStart() {
        if (prefs.isFirstRunComplete || isFinishing) return

        val dialog = AlertDialog.Builder(this).create()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 48, 40, 48)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 20, 29), Color.rgb(20, 42, 53)),
            )
        }
        val logo = TextView(this).apply {
            text = "AP"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(7, 20, 29))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(54, 211, 155))
            }
            layoutParams = LinearLayout.LayoutParams(84, 84).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 28
            }
        }
        val title = TextView(this).apply {
            text = "Autopilot is ready"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val body = TextView(this).apply {
            text = "Tap Start once. We’ll guide you through the Android permissions needed to find your target text and tap it quickly."
            textSize = 16f
            setTextColor(Color.rgb(169, 192, 197))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 32)
        }
        val start = Button(this).apply {
            text = "Start"
            textSize = 16f
            setAllCaps(false)
            setTextColor(Color.rgb(7, 20, 29))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(54, 211, 155))
            layoutParams = LinearLayout.LayoutParams(-1, 58)
        }
        start.setOnClickListener {
            prefs.isFirstRunComplete = true
            dialog.dismiss()
            openAccessibilitySettings()
        }
        content.addView(logo)
        content.addView(title)
        content.addView(body)
        content.addView(start)
        dialog.setView(content)
        dialog.setCancelable(false)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        }
        dialog.show()
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(
            this,
            "Turn on Autopilot Service, then return here.",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun maybeRequestOverlayPermission() {
        if (!prefs.isFirstRunComplete ||
            prefs.hasPromptedForOverlay ||
            !isAccessibilityServiceEnabled() ||
            Settings.canDrawOverlays(this) ||
            isFinishing
        ) return

        prefs.hasPromptedForOverlay = true
        AlertDialog.Builder(this)
            .setTitle("One optional permission")
            .setMessage("Overlay access is only needed for the movable floating Start/Pause controls. Target-text clicking works without it.")
            .setPositiveButton("Grant overlay") { _, _ -> requestOverlayPermission() }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Secure.getString(
            contentResolver,
            Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(this, RideAccepterService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun loadSettings() {
        isUpdatingAccount = true
        toggleSwitch.isChecked = prefs.isEnabled && prefs.hasActiveSubscription()
        // App selection is intentionally not part of the user flow. A blank
        // filter lets the service inspect every foreground user app.
        rapidoModeSwitch.isChecked = false
        olaModeSwitch.isChecked = false
        uberModeSwitch.isChecked = false
        prefs.targetPackage = ""
        customTextInput.setText(prefs.customTexts)
        targetPackageInput.setText("")
        isUpdatingAccount = false
    }

    private fun updateStatus() {
        val enabled = RideAccepterService.isRunning
        val overlay = Settings.canDrawOverlays(this)
        val pkg = prefs.targetPackage
        val mode = when {
            pkg == RAPIDO_PACKAGE -> "RAPIDO"
            pkg == OLA_PACKAGE -> "OLA"
            pkg == UBER_PACKAGE -> "UBER"
            else -> "ALL APPS"
        }
        statusText.text = buildString {
            if (!prefs.hasActiveSubscription()) {
                appendLine("Subscription: EXPIRED")
                appendLine("Watch 10 ads to renew for 1 day")
            }
            appendLine(if (enabled) "Service: RUNNING" else "Service: STOPPED")
            appendLine("Mode: $mode")
            appendLine(if (overlay) "Overlay: GRANTED" else "Overlay: DENIED")
            append(if (prefs.hasActiveSubscription()) "Ready to Auto-Click" else "Auto-click is locked")
        }
        statusText.setTextColor(
            if (enabled && overlay && prefs.hasActiveSubscription()) ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        )
    }

    private fun startStatusAnimation() {
        ObjectAnimator.ofFloat(statusText, "alpha", 0.82f, 1f, 0.82f).apply {
            duration = 2600L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun updateRewardUI() {
        tvRewardProgress.text = "Ads Watched: ${adManager.getRewardProgress()}"
    }

    private fun updateAdStatus() {
        tvAdStatus.text = adManager.getRewardStatusText()
    }

    private fun updateReferralUI() {
        tvReferralCode.text = if (prefs.referralCode.isBlank()) {
            "Your referral code: loading..."
        } else {
            "Your referral code: ${prefs.referralCode}"
        }
    }

    private fun refreshAccount() {
        if (prefs.apiToken.isBlank()) return
        lifecycleScope.launch {
            try {
                prefs.applyRemoteUser(api.me(prefs.apiToken))
                updateReferralUI()
                updateRewardUI()
                updateAdStatus()
                updateStatus()
            } catch (error: Exception) {
                if (error is ApiException && error.statusCode in listOf(401, 403)) {
                    RideAccepterService.isPaused = true
                    prefs.clearSession()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun redeemReferral(code: String) {
        btnRedeemReferral.isEnabled = false
        lifecycleScope.launch {
            try {
                val message = api.redeemReferral(prefs.apiToken, code)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                etReferralCode.text.clear()
                refreshAccount()
            } catch (error: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: "Could not redeem the referral code.",
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                btnRedeemReferral.isEnabled = true
            }
        }
    }

    // --- EXIT INTERSTITIAL ---
    override fun onBackPressed() {
        adManager.showExitInterstitial(this)
        super.onBackPressed()
    }
}
