package com.mamabhutnika.rideaccepter

import com.autopilot.driver.R
import com.autopilot.driver.BuildConfig

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etReferralCode: EditText
    private lateinit var btnSignup: Button
    private lateinit var btnLogin: Button
    private lateinit var prefs: UserPrefs
    private val api = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        prefs = UserPrefs(this)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etReferralCode = findViewById(R.id.etReferralCode)
        btnSignup = findViewById(R.id.btnSignup)
        btnLogin = findViewById(R.id.btnLogin)

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (name.length < 2 ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
                password.length < 6
            ) {
                Toast.makeText(
                    this,
                    "Use a name, valid email, and password with at least 6 characters.",
                    Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            btnSignup.isEnabled = false
            lifecycleScope.launch {
                try {
                    val result = api.signup(
                        name,
                        email,
                        password,
                        etReferralCode.text.toString().trim(),
                    )
                    prefs.apiToken = result.token
                    prefs.isLoggedIn = true
                    prefs.applyRemoteUser(result.user)
                    Toast.makeText(this@SignupActivity, "Account created!", Toast.LENGTH_SHORT).show()
                     val destination = if (result.user.isAdmin) {
                         AdminActivity::class.java
                     } else {
                         MainActivity::class.java
                     }
                     startActivity(Intent(this@SignupActivity, destination))
                    finish()
                } catch (error: Exception) {
                    Toast.makeText(
                        this@SignupActivity,
                        error.message ?: "Could not create the account.",
                        Toast.LENGTH_LONG,
                    ).show()
                } finally {
                    btnSignup.isEnabled = true
                }
            }
        }

        btnLogin.setOnClickListener {
            finish()
        }
    }

    // AD-FREE ZONE: No exit interstitial
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
