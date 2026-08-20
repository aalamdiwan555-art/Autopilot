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
    private var loginInProgress = false

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

        setupListeners()

        validateExistingSession()
    }

    private fun setupListeners() {

        login.setOnClickListener {

            if (loginInProgress) {
                return@setOnClickListener
            }

            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()

            if (emailValue.isBlank()) {
                status.text = "Please enter your email."
                email.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(emailValue)
                    .matches()
            ) {
                status.text = "Please enter a valid email."
                email.requestFocus()
                return@setOnClickListener
            }

            if (passwordValue.length < 6) {
                status.text = "Password must contain at least 6 characters."
                password.requestFocus()
                return@setOnClickListener
            }

            signIn(emailValue, passwordValue)
        }

        signup.setOnClickListener {

            if (loginInProgress) {
                return@setOnClickListener
            }

            startActivity(
                Intent(
                    this,
                    SignupActivity::class.java
                )
            )
        }

        forgot.setOnClickListener {

            if (loginInProgress) {
                return@setOnClickListener
            }

            startActivity(
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )
            )
        }
    }

    private fun signIn(
        emailValue: String,
        passwordValue: String
    ) {

        if (loginInProgress) {
            return
        }

        loginInProgress = true

        setBusy(
            true,
            "Signing in securely…"
        )

        lifecycleScope.launch {

            try {

                val result = withTimeoutOrNull(LOGIN_TIMEOUT_MS) {
                    api.login(
                        emailValue,
                        passwordValue
                    )
                }

                if (result == null) {

                    loginInProgress = false

                    setBusy(
                        false,
                        "Login timed out. Please try again."
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        "Server took too long to respond.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                saveSession(result)

                loginInProgress = false

                setBusy(
                    false,
                    "Login successful. Opening app…"
                )

                openHome(
                    result.user.isAdmin
                )

            } catch (error: Exception) {

                loginInProgress = false

                setBusy(
                    false,
                    "Sign in failed."
                )

                val message =
                    error.message?.takeIf {
                        it.isNotBlank()
                    } ?: "Could not sign in. Please check your email and password."

                Toast.makeText(
                    this@LoginActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validateExistingSession() {

        if (sessionCheckStarted || loginInProgress) {
            return
        }

        if (!prefs.isLoggedIn) {
            return
        }

        if (prefs.apiToken.isBlank()) {
            prefs.clearSession()
            return
        }

        sessionCheckStarted = true

        setBusy(
            true,
            "Checking your saved session…"
        )

        lifecycleScope.launch {

            try {

                val result = withTimeoutOrNull(
                    SESSION_CHECK_TIMEOUT_MS
                ) {
                    runCatching {
                        api.me(
                            prefs.apiToken
                        )
                    }
                }

                val user = result?.getOrNull()

                if (user != null) {

                    prefs.applyRemoteUser(user)

                    setBusy(
                        false,
                        "Session verified. Opening app…"
                    )

                    openHome(
                        user.isAdmin
                    )

                } else {

                    prefs.clearSession()

                    setBusy(
                        false,
                        "Your session expired. Please sign in again."
                    )
                }

            } catch (error: Exception) {

                prefs.clearSession()

                setBusy(
                    false,
                    "Please sign in again."
                )
            }
        }
    }

    private fun saveSession(
        result: AuthResult
    ) {

        val token = result.token.trim()

        if (token.isEmpty()) {
            throw IllegalStateException(
                "Login succeeded but the server returned an empty session token."
            )
        }

        prefs.apiToken = token
        prefs.isLoggedIn = true

        prefs.applyRemoteUser(
            result.user
        )

        // Keep first-time users in onboarding until they complete setup.
        // Logging in and completing device permissions are separate steps.
    }

    private fun openHome(
        isAdmin: Boolean
    ) {

        if (navigationStarted) {
            return
        }

        if (isFinishing) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
            isDestroyed
        ) {
            return
        }

        navigationStarted = true

        val targetActivity: Class<*> = when {

            isAdmin -> {
                AdminActivity::class.java
            }

            !AppPrefs(this).onboarded -> {
                OnboardingActivity::class.java
            }

            else -> {
                MainActivity::class.java
            }
        }

        val intent = Intent(
            this,
            targetActivity
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)

        finish()
    }

    private fun setBusy(
        busy: Boolean,
        message: String
    ) {

        login.isEnabled = !busy
        signup.isEnabled = !busy
        forgot.isEnabled = !busy
        email.isEnabled = !busy
        password.isEnabled = !busy

        status.text = message
    }

    private fun startBreathingAnimation(
        view: View
    ) {

        ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            0f,
            -5f,
            0f
        ).apply {

            duration = 2400L

            repeatCount =
                ObjectAnimator.INFINITE

            interpolator =
                AccelerateDecelerateInterpolator()

            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {

        private const val LOGIN_TIMEOUT_MS =
            15_000L

        private const val SESSION_CHECK_TIMEOUT_MS =
            13_000L
    }
}
