package com.autopilot.driver

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("autopilot", Context.MODE_PRIVATE)
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()
    var rewardDays: Int
        get() = prefs.getInt("reward_days", 0)
        set(value) = prefs.edit().putInt("reward_days", value).apply()
    var rewardCount: Int
        get() = prefs.getInt("reward_count", 0)
        set(value) = prefs.edit().putInt("reward_count", value).apply()
    var captureGranted: Boolean
        get() = prefs.getBoolean("capture_granted", false)
        set(value) = prefs.edit().putBoolean("capture_granted", value).apply()
    var autopilotEnabled: Boolean
        get() = prefs.getBoolean("autopilot_enabled", false)
        set(value) = prefs.edit().putBoolean("autopilot_enabled", value).apply()
    var lastSubscription: String
        get() = prefs.getString("subscription_until", "20 Aug 2026") ?: "20 Aug 2026"
        set(value) = prefs.edit().putString("subscription_until", value).apply()
    fun setAppEnabled(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun appEnabled(key: String): Boolean = prefs.getBoolean(key, true)
}