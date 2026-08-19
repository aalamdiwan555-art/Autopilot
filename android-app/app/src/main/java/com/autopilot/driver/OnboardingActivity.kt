package com.autopilot.driver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OnboardingActivity : AppCompatActivity() {
    private lateinit var prefs: AppPrefs
    private lateinit var continueButton: Button
    private val rows = mutableListOf<Pair<TextView, () -> Boolean>>()

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            prefs.captureGranted = true
            val serviceIntent = Intent(this, ScreenReaderService::class.java).apply {
                putExtra(ScreenReaderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenReaderService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= 26) {
                androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Screen capture is required to read ride details.", Toast.LENGTH_SHORT).show()
        }
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        if (prefs.onboarded) {
            openMain()
            return
        }
        setContentView(buildScreen())
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::continueButton.isInitialized) refresh()
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun buildScreen(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(color(R.color.autopilot_background)) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(28))
        }
        column.addView(TextView(this).apply {
            text = "AUTOPILOT"
            textSize = 12f
            setTextColor(color(R.color.autopilot_primary))
            letterSpacing = .16f
        })
        column.addView(TextView(this).apply {
            text = "Set up your\nride desk."
            textSize = 34f
            setTextColor(color(R.color.autopilot_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(8))
        })
        column.addView(TextView(this).apply {
            text = "Autopilot needs five permissions before it can keep you ready for rides. You stay in control at every step."
            textSize = 15f
            setTextColor(color(R.color.autopilot_muted))
            setPadding(0, 0, 0, dp(22))
        })
        permissionRow(column, "Accessibility Service", "To auto click buttons in Rapido, Ola, and Uber", android.R.drawable.ic_menu_manage,
            { openAccessibility() }, { RideAccessibilityService.isEnabled(this) })
        permissionRow(column, "Floating Control", "To show the floating button above ride apps", android.R.drawable.ic_menu_view,
            { openOverlay() }, { Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this) })
        permissionRow(column, "Screen Capture", "To read ride details using on-device OCR", android.R.drawable.ic_menu_camera,
            { requestCapture() }, { prefs.captureGranted })
        permissionRow(column, "Notification Access", "To detect new ride notifications", android.R.drawable.ic_dialog_info,
            { requestNotifications() }, { notificationGranted() })
        permissionRow(column, "Internet", "For ads and subscription", android.R.drawable.ic_menu_upload,
            { }, { true })

        continueButton = Button(this).apply {
            text = "Continue to App"
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.onboarded = true
                openMain()
            }
        }
        column.addView(continueButton, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(20), 0, 0) })
        column.addView(TextView(this).apply {
            text = "You can review these permissions later from the Setup card."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.autopilot_muted))
            setPadding(0, dp(12), 0, 0)
        })
        scroll.addView(column)
        return scroll
    }

    private fun permissionRow(
        parent: LinearLayout,
        title: String,
        description: String,
        icon: Int,
        action: () -> Unit,
        check: () -> Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(10), dp(12))
            setBackgroundColor(color(R.color.autopilot_card))
            isClickable = true
            setOnClickListener { action() }
        }
        val image = ImageView(this).apply {
            setImageResource(icon)
            setColorFilter(color(R.color.autopilot_primary))
        }
        card.addView(image, LinearLayout.LayoutParams(dp(30), dp(30)))
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        copy.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(color(R.color.autopilot_text))
        })
        copy.addView(TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(color(R.color.autopilot_muted))
            setPadding(0, dp(3), 0, 0)
        })
        card.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        val status = TextView(this).apply { textSize = 22f; gravity = Gravity.CENTER }
        card.addView(status, LinearLayout.LayoutParams(dp(32), dp(40)))
        rows += status to check
        parent.addView(card, LinearLayout.LayoutParams(-1, dp(78)).apply { setMargins(0, 0, 0, dp(10)) })
    }

    private fun refresh() {
        rows.forEach { (status, check) ->
            val granted = runCatching { check() }.getOrDefault(false)
            status.text = if (granted) "✓" else "›"
            status.setTextColor(color(if (granted) R.color.autopilot_success else R.color.autopilot_muted))
        }
        continueButton.isEnabled = rows.all { runCatching { it.second() }.getOrDefault(false) }
        continueButton.alpha = if (continueButton.isEnabled) 1f else .45f
        continueButton.setBackgroundColor(color(R.color.autopilot_primary))
    }

    private fun openAccessibility() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    private fun openOverlay() {
        if (Build.VERSION.SDK_INT >= 23) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }
    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
    }
    private fun notificationGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}