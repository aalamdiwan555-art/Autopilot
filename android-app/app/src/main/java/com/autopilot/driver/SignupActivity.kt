package com.autopilot.driver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autopilot.driver.R
import com.mamabhutnika.rideaccepter.AdminActivity
import com.mamabhutnika.rideaccepter.ApiClient
import com.mamabhutnika.rideaccepter.UserPrefs
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var referral: EditText
    private lateinit var signup: Button
    private lateinit var login: Button
    private val api = ApiClient()
    private lateinit var prefs: UserPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        prefs = UserPrefs(this)
        name = findViewById(R.id.etName)
        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        referral = findViewById(R.id.etReferralCode)
        signup = findViewById(R.id.btnSignup)
        login = findViewById(R.id.btnLogin)
        animateLogo(findViewById(R.id.tvLogo))

        signup.setOnClickListener {
            val nameValue = name.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()
            if (nameValue.length < 2 ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() ||
                passwordValue.length < 6
            ) {
                Toast.makeText(this, "Use a name, valid email, and password with at least 6 characters.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            setBusy(true)
            lifecycleScope.launch {
                try {
                    val result = api.signup(nameValue, emailValue, passwordValue, referral.text.toString().trim())
                    prefs.apiToken = result.token
                    prefs.isLoggedIn = true
                    prefs.applyRemoteUser(result.user)
                    val target = when {
                        !AppPrefs(this@SignupActivity).onboarded -> OnboardingActivity::class.java
                        result.user.isAdmin -> AdminActivity::class.java
                        else -> MainActivity::class.java
                    }
                    startActivity(Intent(this@SignupActivity, target).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                } catch (error: Exception) {
                    setBusy(false)
                    Toast.makeText(this@SignupActivity, error.message ?: "Could not create the account.", Toast.LENGTH_LONG).show()
                }
            }
        }
        login.setOnClickListener { finish() }
    }

    private fun setBusy(busy: Boolean) {
        signup.isEnabled = !busy
        login.isEnabled = !busy
        name.isEnabled = !busy
        email.isEnabled = !busy
        password.isEnabled = !busy
        referral.isEnabled = !busy
        signup.text = if (busy) "Creating account…" else "Create account"
    }

    private fun animateLogo(view: View) {
        view.animate().translationY(-6f).setDuration(2200L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate().translationY(0f).setDuration(2200L)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }.start()
    }
}