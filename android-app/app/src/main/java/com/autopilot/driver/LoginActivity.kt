package com.autopilot.driver

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autopilot.driver.R
import com.mamabhutnika.rideaccepter.AdminActivity
import com.mamabhutnika.rideaccepter.ApiClient
import com.mamabhutnika.rideaccepter.AuthResult
import com.mamabhutnika.rideaccepter.UserPrefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var signup: Button
    private lateinit var forgot: TextView
    private lateinit var status: TextView
    private lateinit var logo: View
    private lateinit var prefs: UserPrefs
    private val api = ApiClient()
    private var sessionCheckStarted = false
    private var navigationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prefs = UserPrefs(this)

        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        login = findViewById(R.id.btnLogin)
        signup = findViewById(R.id.btnSignup)
        forgot = findViewById(R.id.tvForgotPassword)
        status = findViewById(R.id.tvTrialStatus)
        logo = findViewById(R.id.tvLogo)

        startBreathingAnimation(logo)
        login.setOnClickListener {
            val value = email.text.toString().trim()
            val secret = password.text.toString()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() || secret.length < 6) {
                status.text = "Enter a valid email and a password with 6+ characters."
                return@setOnClickListener
            }
            signIn(value, secret)
        }
        signup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        forgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        validateExistingSession()
    }

    private fun signIn(emailValue: String, passwordValue: String) {
        setBusy(true, "Signing in securely…")
        lifecycleScope.launch {
            try {
                val result = api.login(emailValue, passwordValue)
                saveSession(result)
                openHome(result.user.isAdmin)
            } catch (error: Exception) {
                setBusy(false, "Sign in failed")
                Toast.makeText(this@LoginActivity, error.message ?: "Could not sign in.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateExistingSession() {
        if (sessionCheckStarted || !prefs.isLoggedIn || prefs.apiToken.isBlank()) return
        sessionCheckStarted = true
        setBusy(true, "Checking your saved session…")
        lifecycleScope.launch {
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
        }
    }

    private fun saveSession(result: AuthResult) {
        prefs.apiToken = result.token
        prefs.isLoggedIn = true
        prefs.applyRemoteUser(result.user)
    }

    private fun openHome(isAdmin: Boolean) {
        if (navigationStarted || isFinishing || (Build.VERSION.SDK_INT >= 17 && isDestroyed)) return
        navigationStarted = true
        val target = when {
            !AppPrefs(this).onboarded -> OnboardingActivity::class.java
            isAdmin -> AdminActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }

    private fun setBusy(busy: Boolean, message: String) {
        login.isEnabled = !busy
        signup.isEnabled = !busy
        forgot.isEnabled = !busy
        email.isEnabled = !busy
        password.isEnabled = !busy
        status.text = message
    }

    private fun startBreathingAnimation(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -5f, 0f).apply {
            duration = 2400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    companion object {
        private const val SESSION_CHECK_TIMEOUT_MS = 13_000L
    }
}