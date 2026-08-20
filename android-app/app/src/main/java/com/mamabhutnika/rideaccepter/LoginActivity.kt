package com.mamabhutnika.rideaccepter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirect directly to the main LoginActivity
        val intent = Intent(this, com.autopilot.driver.LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }
}
