package com.mamabhutnika.rideaccepter

import com.autopilot.driver.R
import com.autopilot.driver.BuildConfig

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

open class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button
    private lateinit var tvTrialStatus: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvLogo: View
    private lateinit var tvForgotPassword: TextView
    private lateinit var prefs: UserPrefs
    private val api = ApiClient()
    private var sessionCheckStarted = false
    private var navigationStarted = false

    companion object {
        private const val SESSION_CHECK_TIMEOUT_MS = 13_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = UserPrefs(this)
        
        // Connect all views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignup = findViewById(R.id.btnSignup)
        tvTrialStatus = findViewById(R.id.tvTrialStatus)
        tvVersion = findViewById(R.id.tvVersion)
        tvLogo = findViewById(R.id.tvLogo)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        tvVersion.text = "Autopilot • v4.0"
        tvTrialStatus.text = "Sign in to check your subscription"
        startBreathingAnimation(tvLogo)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 6) {
                Toast.makeText(this, "Enter a valid email and password (6+ characters)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signIn(email, password)
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, com.autopilot.driver.SignupActivity::class.java))
        }
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, com.autopilot.driver.ForgotPasswordActivity::class.java))
        }

        validateExistingSession()
    }

    private fun signIn(email: String, password: String) {
        setBusy(true, "Signing in securely…")
        lifecycleScope.launch {
            try {
                val result = api.login(email, password)
                saveSession(result)
                openHome(result.user.isAdmin)
            } catch (error: Exception) {
                tvTrialStatus.text = "Sign in failed"
                Toast.makeText(this@LoginActivity, error.message ?: "Could not sign in.", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false, "Sign in to check your subscription")
            }
        }
    }

    private fun validateExistingSession() {
        if (sessionCheckStarted || !prefs.isLoggedIn || prefs.apiToken.isBlank()) return
        sessionCheckStarted = true
        setBusy(true, "Checking your saved session…")
        lifecycleScope.launch {
            try {
                val result = withTimeoutOrNull(SESSION_CHECK_TIMEOUT_MS) {
                    runCatching { api.me(prefs.apiToken) }
                }
                val user = result?.getOrNull()
                if (user != null) {
                    prefs.applyRemoteUser(user)
                    openHome(user.isAdmin)
                } else {
                    prefs.clearSession()
                    setBusy(false, "Your session expired. Please sign in again.")
                }
            } catch (error: Exception) {
                prefs.clearSession()
                setBusy(false, error.message ?: "Your session expired. Please sign in again.")
            }
        }
    }

    private fun saveSession(result: AuthResult) {
        prefs.apiToken = result.token
        prefs.isLoggedIn = true
        prefs.applyRemoteUser(result.user)
    }

    private fun openHome(isAdmin: Boolean) {
        if (navigationStarted || isFinishing || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed)) return
        navigationStarted = true
        val destination = if (!com.autopilot.driver.AppPrefs(this).onboarded) {
            com.autopilot.driver.OnboardingActivity::class.java
        } else if (isAdmin) {
            AdminActivity::class.java
        } else {
            com.autopilot.driver.MainActivity::class.java
        }
        startActivity(Intent(this, destination).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }

    private fun setBusy(busy: Boolean, status: String) {
        btnLogin.isEnabled = !busy
        btnSignup.isEnabled = !busy
        etEmail.isEnabled = !busy
        etPassword.isEnabled = !busy
        tvTrialStatus.text = status
    }

    private fun startBreathingAnimation(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -5f, 0f).apply {
            duration = 2400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
