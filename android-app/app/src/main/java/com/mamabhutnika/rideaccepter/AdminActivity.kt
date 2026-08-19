package com.mamabhutnika.rideaccepter

import com.autopilot.driver.R
import com.autopilot.driver.BuildConfig

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var tvAdminTitle: TextView
    private lateinit var tvCurrentUser: TextView
    private lateinit var grantDaysInput: EditText
    private lateinit var btnRefreshUsers: Button
    private lateinit var btnClearAll: Button
    private lateinit var btnLogout: Button
    private lateinit var usersContainer: LinearLayout
    private lateinit var globalMatchingSwitch: SwitchCompat
    private lateinit var prefs: UserPrefs
    private lateinit var adManager: AdManager
    private val api = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        prefs = UserPrefs(this)
        adManager = AdManager.getInstance(this)

        tvAdminTitle = findViewById(R.id.tvAdminTitle)
        tvCurrentUser = findViewById(R.id.tvCurrentUser)
        grantDaysInput = findViewById(R.id.grantDaysInput)
        btnRefreshUsers = findViewById(R.id.btnRefreshUsers)
        btnClearAll = findViewById(R.id.btnClearAll)
        btnLogout = findViewById(R.id.btnLogout)
        usersContainer = findViewById(R.id.usersContainer)
        globalMatchingSwitch = findViewById(R.id.globalMatchingSwitch)
        globalMatchingSwitch.isChecked = prefs.globalAdvancedMatching
        globalMatchingSwitch.setOnCheckedChangeListener { _, enabled ->
            prefs.globalAdvancedMatching = enabled
            Toast.makeText(
                this,
                if (enabled) "Advanced matching is enabled for active users." else "Advanced matching is disabled globally.",
                Toast.LENGTH_SHORT,
            ).show()
        }

        tvAdminTitle.text = "ADMIN PANEL"
        tvCurrentUser.text = "Your UID: ${prefs.uid}\nEmail: ${prefs.userEmail}"

        btnRefreshUsers.setOnClickListener {
            refreshUserList()
        }

        btnClearAll.setOnClickListener {
            prefs.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnLogout.setOnClickListener {
            prefs.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        refreshUserList()
    }

    private fun refreshUserList() {
        usersContainer.removeAllViews()
        lifecycleScope.launch {
            try {
                val users = api.adminUsers(prefs.apiToken)
                if (users.isEmpty()) {
                    addEmptyState("No users registered yet.")
                    return@launch
                }
                for (user in users) {
                    val userRow = LinearLayout(this@AdminActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(8, 12, 8, 12)
                    }
                    val tvInfo = TextView(this@AdminActivity).apply {
                        text = buildString {
                            append("UID: ${user.uid}\n")
                            append("Email: ${user.email}\n")
                            append("Subscription: ")
                            append(if (user.subscriptionActive) user.subscriptionUntilMs.asDate() else "EXPIRED")
                        }
                        setTextColor(ContextCompat.getColor(this@AdminActivity, android.R.color.white))
                        textSize = 12f
                    }
                    val grantButton = Button(this@AdminActivity).apply {
                        text = "GRANT DAYS TO ${user.uid}"
                        setOnClickListener { grantDays(user.uid, this) }
                    }
                    userRow.addView(tvInfo)
                    userRow.addView(grantButton)
                    usersContainer.addView(userRow)
                }
            } catch (error: Exception) {
                if (error.message?.contains("Admin access required", ignoreCase = true) == true ||
                    error.message?.contains("session", ignoreCase = true) == true
                ) {
                    prefs.clearSession()
                    startActivity(Intent(this@AdminActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }
                addEmptyState(error.message ?: "Could not load users.")
            }
        }
    }

    private fun grantDays(uid: String, button: Button) {
        val days = grantDaysInput.text.toString().trim().toIntOrNull()
        if (days == null || days !in 1..3650) {
            Toast.makeText(this, "Enter 1 to 3650 days first.", Toast.LENGTH_SHORT).show()
            return
        }
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                api.grantDays(prefs.apiToken, uid, days)
                Toast.makeText(this@AdminActivity, "$days days granted to $uid.", Toast.LENGTH_LONG).show()
                refreshUserList()
            } catch (error: Exception) {
                Toast.makeText(
                    this@AdminActivity,
                    error.message ?: "Could not grant subscription.",
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                button.isEnabled = true
            }
        }
    }

    private fun addEmptyState(message: String) {
        val emptyText = TextView(this)
        emptyText.text = message
        emptyText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        emptyText.textSize = 14f
        usersContainer.addView(emptyText)
    }

    private fun Long.asDate(): String {
        return java.text.DateFormat.getDateTimeInstance().format(java.util.Date(this))
    }

    // AD-FREE ZONE: No exit interstitial
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
