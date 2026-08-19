package com.mamabhutnika.rideaccepter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autopilot.driver.R
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var reset: Button
    private lateinit var back: Button
    private lateinit var status: TextView
    private val api = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        email = findViewById(R.id.etEmail)
        reset = findViewById(R.id.btnReset)
        back = findViewById(R.id.btnBack)
        status = findViewById(R.id.tvStatus)
        findViewById<View>(R.id.tvLogo).animate()
            .translationY(-6f)
            .setDuration(1800L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                findViewById<View>(R.id.tvLogo).animate()
                    .translationY(0f)
                    .setDuration(1800L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }.start()

        reset.setOnClickListener {
            val value = email.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                status.text = "Enter a valid email address."
                return@setOnClickListener
            }
            reset.isEnabled = false
            status.text = "Sending a secure reset link…"
            lifecycleScope.launch {
                try {
                    status.text = api.requestPasswordReset(value)
                } catch (error: Exception) {
                    status.text = error.message ?: "Could not send the reset link."
                } finally {
                    reset.isEnabled = true
                }
            }
        }
        back.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}