package com.mamabhutnika.rideaccepter

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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

        // Overlay permission prompt
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Autopilot needs Display over other apps permission for floating controls.")
                .setPositiveButton("Grant") { _, _ -> requestOverlayPermission() }
                .setNegativeButton("Later", null)
                .show()
        }
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
    }

    private fun loadSettings() {
        isUpdatingAccount = true
        toggleSwitch.isChecked = prefs.isEnabled && prefs.hasActiveSubscription()
        rapidoModeSwitch.isChecked = prefs.isRapidoMode
        olaModeSwitch.isChecked = prefs.isOlaMode
        uberModeSwitch.isChecked = prefs.isUberMode
        customTextInput.setText(prefs.customTexts)
        targetPackageInput.setText(prefs.targetPackage)
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
