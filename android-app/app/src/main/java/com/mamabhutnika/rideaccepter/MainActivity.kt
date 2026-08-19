package com.mamabhutnika.rideaccepter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    companion object {
        const val RAPIDO_PACKAGE = "com.rapido.rider"
        const val OLA_PACKAGE = "com.olacabs.oladriver"
        const val UBER_PACKAGE = "com.ubercab.driver"
        private const val PERMISSION_REQUEST = 40
    }

    private lateinit var prefs: UserPrefs
    private lateinit var adManager: AdManager
    private lateinit var page: LinearLayout
    private lateinit var bannerTop: LinearLayout
    private lateinit var bannerBottom: LinearLayout
    private lateinit var statusTitle: TextView
    private lateinit var subscriptionText: TextView
    private lateinit var rewardText: TextView
    private lateinit var serviceButton: Button
    private lateinit var permissionsButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var overlayButton: Button
    private lateinit var referPage: LinearLayout
    private lateinit var homePage: LinearLayout
    private val api = ApiClient()
    private var accountRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = UserPrefs(this)
        adManager = AdManager.getInstance(this)
        setContentView(buildScreen())
        adManager.attachBanner(bannerTop)
        adManager.attachBanner(bannerBottom)
        refreshHome()
        requestFriendlyPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionsButton.isInitialized) refreshPermissionState()
    }

    private fun buildScreen(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(7, 20, 29)) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), 0)
        }
        bannerTop = LinearLayout(this)
        column.addView(bannerTop, LinearLayout.LayoutParams(-1, dp(52)))
        val scroll = ScrollView(this).apply { isFillViewport = true }
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        homePage = buildHome()
        referPage = buildRefer()
        page.addView(homePage)
        scroll.addView(page)
        column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        bannerBottom = LinearLayout(this)
        column.addView(bannerBottom, LinearLayout.LayoutParams(-1, dp(52)))
        column.addView(buildNavigation(), LinearLayout.LayoutParams(-1, dp(68)))
        root.addView(column)
        return root
    }

    private fun buildHome(): LinearLayout {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(label("AUTOPILOT", 12, "#36D39B"))
        layout.addView(title("Your ride desk", 28))
        layout.addView(label("A quieter way to stay ready for every shift.", 14, "#A9C0C5"))
        layout.addView(space(18))

        val statusCard = card()
        statusCard.addView(label("AUTOMATION STATUS", 11, "#36D39B"))
        statusTitle = label("Checking your ride desk…", 16, "#F5FBF8")
        statusCard.addView(statusTitle)
        serviceButton = button("Start autopilot", "#36D39B")
        serviceButton.setOnClickListener { toggleAutomation() }
        statusCard.addView(serviceButton)
        layout.addView(statusCard)

        val subscriptionCard = card()
        subscriptionCard.addView(label("SUBSCRIPTION & REWARDS", 11, "#F7B955"))
        subscriptionText = label("", 14, "#F5FBF8")
        rewardText = label("", 13, "#A9C0C5")
        subscriptionCard.addView(subscriptionText)
        subscriptionCard.addView(rewardText)
        val rewardButton = button("Watch an ad · earn a day", "#3FA9F5")
        rewardButton.setOnClickListener { watchRewardedAd() }
        subscriptionCard.addView(rewardButton)
        layout.addView(subscriptionCard)

        val setupCard = card()
        setupCard.addView(label("ONE-TIME SETUP", 11, "#36D39B"))
        setupCard.addView(label("Grant the permissions Autopilot needs to work reliably. We’ll explain each one before asking.", 14, "#A9C0C5"))
        permissionsButton = button("Review permissions", "#275362").apply { setOnClickListener { requestFriendlyPermissions(true) } }
        setupCard.addView(permissionsButton)
        accessibilityButton = button("Enable accessibility service", "#275362").apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        setupCard.addView(accessibilityButton)
        overlayButton = button("Enable floating control", "#275362").apply {
            setOnClickListener { openOverlaySettings() }
        }
        setupCard.addView(overlayButton)
        layout.addView(setupCard)

        val modesCard = card()
        modesCard.addView(label("SUPPORTED APPS", 11, "#36D39B"))
        modesCard.addView(label("Choose where autopilot listens. Advanced matching is managed globally by admins and is intentionally not shown here.", 14, "#A9C0C5"))
        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modes.addView(modeButton("Rapido", UserPrefs.KEY_RAPIDO_MODE), LinearLayout.LayoutParams(0, dp(48), 1f))
        modes.addView(modeButton("Ola", UserPrefs.KEY_OLA_MODE), LinearLayout.LayoutParams(0, dp(48), 1f))
        modes.addView(modeButton("Uber", UserPrefs.KEY_UBER_MODE), LinearLayout.LayoutParams(0, dp(48), 1f))
        modesCard.addView(modes)
        layout.addView(modesCard)
        layout.addView(card().apply {
            addView(label("ABOUT AUTOPILOT", 11, "#36D39B"))
            addView(label("A focused ride companion built for drivers who want fewer taps and more time on the road.", 14, "#A9C0C5"))
            addView(button("Open subscription & support", "#275362").apply { setOnClickListener { showAboutDialog() } })
        })
        return layout
    }

    private fun buildRefer(): LinearLayout {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(label("REFER & EARN", 12, "#36D39B"))
        layout.addView(title("Bring your crew", 28))
        layout.addView(label("Share Autopilot with another driver and earn rewards together.", 14, "#A9C0C5"))
        layout.addView(space(18))
        val card = card()
        card.addView(label("YOUR REFERRAL CODE", 11, "#F7B955"))
        val code = label(if (prefs.referralCode.isBlank()) "Loading…" else prefs.referralCode, 30, "#F5FBF8").apply {
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }
        card.addView(code)
        card.addView(button("Share referral link", "#36D39B").apply { setOnClickListener { shareReferral() } })
        val analytics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        analytics.addView(metric("0", "Invites"), LinearLayout.LayoutParams(0, dp(72), 1f))
        analytics.addView(metric("${prefs.totalAdsWatched}", "Rewards"), LinearLayout.LayoutParams(0, dp(72), 1f))
        card.addView(analytics)
        layout.addView(card)
        layout.addView(card().apply {
            addView(label("HAVE A CODE?", 11, "#36D39B"))
            val input = EditText(this@MainActivity).apply {
                hint = "Enter a friend’s code"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.rgb(117, 144, 151))
                setSingleLine()
            }
            addView(input)
            addView(button("Redeem code", "#3FA9F5").apply {
                setOnClickListener { redeemReferral(input) }
            })
        })
        return layout
    }

    private fun buildNavigation(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(10))
        }
        nav.addView(navButton("Home") { showPage(homePage) }, LinearLayout.LayoutParams(0, -1, 1f))
        nav.addView(navButton("Refer & Earn") { showPage(referPage) }, LinearLayout.LayoutParams(0, -1, 1f))
        nav.addView(navButton("Profile") { showAboutDialog() }, LinearLayout.LayoutParams(0, -1, 1f))
        return nav
    }

    private fun showPage(view: View) {
        page.removeAllViews()
        page.addView(view)
        adManager.showInterstitial(this)
    }

    private fun toggleAutomation() {
        prefs.isEnabled = !prefs.isEnabled
        refreshHome()
        Toast.makeText(this, if (prefs.isEnabled) "Autopilot is ready." else "Autopilot paused.", Toast.LENGTH_SHORT).show()
    }

    private fun refreshHome() {
        if (!::statusTitle.isInitialized) return
        val active = prefs.hasActiveSubscription()
        statusTitle.text = if (prefs.isEnabled && active) "Running · ready to auto-accept" else if (!active) "Subscription needed to start autopilot" else "Paused · tap start when ready"
        serviceButton.text = if (prefs.isEnabled) "Pause autopilot" else "Start autopilot"
        subscriptionText.text = if (active) "Active until ${DateFormat.getDateInstance().format(Date(prefs.subscriptionUntilMs))}" else "Inactive · watch 10 ads to unlock one day"
        rewardText.text = "Reward progress: ${prefs.rewardCounter}/${UserPrefs.REWARD_THRESHOLD} · ${prefs.totalAdsWatched} total ads watched"
    }

    private fun watchRewardedAd() {
        adManager.showRewardedVideo(this, { refreshHome() }, { Toast.makeText(this, "The ad was not available. We’ll retry shortly.", Toast.LENGTH_LONG).show() })
    }

    private fun shareReferral() {
        val code = prefs.referralCode.ifBlank { "AUTOPILOT" }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join me on Autopilot. Use my referral code $code.")
        }, "Share with a driver"))
    }

    private fun redeemReferral(input: EditText) {
        val code = input.text.toString().trim()
        if (code.isBlank() || prefs.apiToken.isBlank()) return
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, api.redeemReferral(prefs.apiToken, code), Toast.LENGTH_LONG).show()
                input.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message ?: "Could not redeem code.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Autopilot")
            .setMessage("Your subscription keeps automation active. Rewarded videos can extend your access. Standard ads help keep the app available.\n\nTerms of Service · Privacy Policy · support@autopilot.app")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun requestFriendlyPermissions(showDialog: Boolean = false) {
        val permissions = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isEmpty()) {
            refreshPermissionState()
            if (showDialog) Toast.makeText(this, "App permissions are already allowed.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!showDialog && prefs.isFirstRunComplete) return
        AlertDialog.Builder(this)
            .setTitle("A few permissions, explained")
            .setMessage("Autopilot uses these only for the features you enable. You can deny any permission and continue, then change it later in Settings.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Continue") { _, _ ->
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST)
                prefs.isFirstRunComplete = true
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST) return
        refreshPermissionState()
        val denied = grantResults.count { it != PackageManager.PERMISSION_GRANTED }
        if (denied > 0) {
            Toast.makeText(
                this,
                "$denied permission(s) still need approval. You can change them anytime in Android Settings.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun refreshPermissionState() {
        if (!::permissionsButton.isInitialized) return
        val missing = requestedPermissions().count {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        permissionsButton.text = if (missing == 0) "Permissions ready" else "Review permissions · $missing remaining"
        permissionsButton.setTextColor(if (missing == 0) Color.rgb(54, 211, 155) else Color.WHITE)
        accessibilityButton.text = if (isAccessibilityEnabled()) "Accessibility service enabled" else "Enable accessibility service"
        accessibilityButton.setTextColor(if (isAccessibilityEnabled()) Color.rgb(54, 211, 155) else Color.WHITE)
        val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        overlayButton.text = if (overlayAllowed) "Floating control enabled" else "Enable floating control"
        overlayButton.setTextColor(if (overlayAllowed) Color.rgb(54, 211, 155) else Color.WHITE)
    }

    private fun requestedPermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals("${packageName}/${RideAccepterService::class.java.name}", ignoreCase = true) }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else Toast.makeText(this, "Floating control is already allowed.", Toast.LENGTH_SHORT).show()
    }

    private fun modeButton(text: String, key: String): Button = button(text, "#275362").apply {
        setOnClickListener {
            val value = when (key) {
                UserPrefs.KEY_RAPIDO_MODE -> !prefs.isRapidoMode
                UserPrefs.KEY_OLA_MODE -> !prefs.isOlaMode
                else -> !prefs.isUberMode
            }
            when (key) {
                UserPrefs.KEY_RAPIDO_MODE -> prefs.isRapidoMode = value
                UserPrefs.KEY_OLA_MODE -> prefs.isOlaMode = value
                else -> prefs.isUberMode = value
            }
            setTextColor(if (value) Color.rgb(54, 211, 155) else Color.WHITE)
        }
    }

    private fun metric(value: String, caption: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(label(value, 20, "#F5FBF8"))
        addView(label(caption, 12, "#A9C0C5"))
    }

    private fun navButton(text: String, onClick: () -> Unit) = button(text, "#142A35").apply { setOnClickListener { onClick() } }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(Color.rgb(20, 42, 53))
        layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
    }
    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        layoutParams = LinearLayout.LayoutParams(-1, dp(46)).apply { setMargins(0, dp(10), 0, 0) }
    }
    private fun label(text: String, size: Int, color: String) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(3), 0, dp(3))
    }
    private fun title(text: String, size: Int) = label(text, size, "#F5FBF8").apply { typeface = Typeface.DEFAULT_BOLD }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}