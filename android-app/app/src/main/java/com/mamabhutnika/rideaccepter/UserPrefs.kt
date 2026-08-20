package com.mamabhutnika.rideaccepter

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.*

class UserPrefs(context: Context) {

    companion object {
        const val PREFS_NAME = "mama_bhutnika_prefs"
        const val KEY_UID = "user_uid"
        const val KEY_VERSION_TAP_COUNT = "version_tap_count"
        const val KEY_IS_ADMIN = "is_admin"

        // Auth
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_API_TOKEN = "api_token"
        const val KEY_REFERRAL_CODE = "referral_code"
        const val KEY_SUBSCRIPTION_UNTIL_MS = "subscription_until_ms"

        // Ad-Free
        const val KEY_IS_AD_FREE = "is_ad_free"
        const val KEY_ADMIN_AD_FREE = "admin_ad_free_override"
        const val KEY_FIRST_RUN_COMPLETE = "first_run_complete"
        const val KEY_OVERLAY_PROMPTED = "overlay_prompted"

        // Trial
        const val KEY_TRIAL_START_MS = "trial_start_ms"
        const val KEY_TRIAL_DURATION_MS = "trial_duration_ms"

        // Reward
        const val KEY_REWARD_COUNTER = "reward_counter"
        const val KEY_REWARD_EXPIRY_MS = "reward_expiry_ms"
        const val KEY_TOTAL_ADS_WATCHED = "total_ads_watched"

        // App Settings
        const val KEY_ENABLED = "enabled"
        const val KEY_CUSTOM_TEXTS = "custom_texts"
        const val KEY_TARGET_PACKAGE = "target_package"
        const val KEY_RAPIDO_MODE = "rapido_mode"
        const val KEY_OLA_MODE = "ola_mode"
        const val KEY_UBER_MODE = "uber_mode"
        const val KEY_GLOBAL_ADVANCED_MATCHING = "global_advanced_matching"

        // Admin user list (stored as comma-separated "uid:email:adfree")
        const val KEY_ADMIN_USER_LIST = "admin_user_list"

        const val ONE_HOUR_MS = 3_600_000L
        const val ONE_DAY_MS = 86_400_000L
        const val REWARD_THRESHOLD = 10
        const val VERSION_TAP_THRESHOLD = 10
    }

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    // --- UID ---
    val uid: String
        get() {
            var id = prefs.getString(KEY_UID, null)
            if (id == null) {
                id = UUID.randomUUID().toString().substring(0, 8).uppercase()
                prefs.edit().putString(KEY_UID, id).apply()
            }
            return id
        }

    // --- Version Tap (Secret Admin Entry) ---
    var versionTapCount: Int
        get() = prefs.getInt(KEY_VERSION_TAP_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_VERSION_TAP_COUNT, value).apply()

    fun incrementVersionTap(): Boolean {
        val newCount = versionTapCount + 1
        versionTapCount = newCount
        return newCount >= VERSION_TAP_THRESHOLD
    }

    fun resetVersionTap() {
        versionTapCount = 0
    }

    // --- Admin ---
    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ADMIN, value).apply()

    // --- Auth ---
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var apiToken: String
        get() = prefs.getString(KEY_API_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_TOKEN, value).apply()

    var referralCode: String
        get() = prefs.getString(KEY_REFERRAL_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REFERRAL_CODE, value).apply()

    var subscriptionUntilMs: Long
        get() = prefs.getLong(KEY_SUBSCRIPTION_UNTIL_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_SUBSCRIPTION_UNTIL_MS, value).apply()

    fun applyRemoteUser(user: RemoteUser) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_REFERRAL_CODE, user.referralCode)
            .putLong(KEY_SUBSCRIPTION_UNTIL_MS, user.subscriptionUntilMs)
            .putInt(KEY_REWARD_COUNTER, user.rewardProgress)
            .putInt(KEY_TOTAL_ADS_WATCHED, user.totalAdsWatched)
            .putBoolean(KEY_IS_ADMIN, user.isAdmin)
            .apply()
    }

    fun hasActiveSubscription(): Boolean {
        return adminAdFreeOverride || subscriptionUntilMs > System.currentTimeMillis()
    }

    fun canUseAutoClicker(): Boolean {
        return isLoggedIn && isEnabled && hasActiveSubscription()
    }

    // --- Ad-Free ---
    var isAdFree: Boolean
        get() = prefs.getBoolean(KEY_IS_AD_FREE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_AD_FREE, value).apply()

    var adminAdFreeOverride: Boolean
        get() = prefs.getBoolean(KEY_ADMIN_AD_FREE, false)
        set(value) = prefs.edit().putBoolean(KEY_ADMIN_AD_FREE, value).apply()

    /**
     * The onboarding gate is intentionally separate from login state. A returning
     * user should never be trapped in the first-run permission explanation again.
     */
    var isFirstRunComplete: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN_COMPLETE, value).apply()

    var hasPromptedForOverlay: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_PROMPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_PROMPTED, value).apply()

    // --- Trial ---
    var trialStartMs: Long
        get() = prefs.getLong(KEY_TRIAL_START_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_TRIAL_START_MS, value).apply()

    var trialDurationMs: Long
        get() = prefs.getLong(KEY_TRIAL_DURATION_MS, ONE_HOUR_MS)
        set(value) = prefs.edit().putLong(KEY_TRIAL_DURATION_MS, value).apply()

    fun startTrial() {
        if (trialStartMs == 0L) {
            trialStartMs = System.currentTimeMillis()
        }
    }

    fun isInTrialPeriod(): Boolean {
        if (trialStartMs == 0L) return false
        return System.currentTimeMillis() < trialStartMs + trialDurationMs
    }

    fun getTrialRemainingMs(): Long {
        if (trialStartMs == 0L) return 0L
        val remaining = (trialStartMs + trialDurationMs) - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    // --- Reward System ---
    var rewardCounter: Int
        get() = prefs.getInt(KEY_REWARD_COUNTER, 0)
        set(value) = prefs.edit().putInt(KEY_REWARD_COUNTER, value).apply()

    var rewardExpiryMs: Long
        get() = prefs.getLong(KEY_REWARD_EXPIRY_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_REWARD_EXPIRY_MS, value).apply()

    var totalAdsWatched: Int
        get() = prefs.getInt(KEY_TOTAL_ADS_WATCHED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_ADS_WATCHED, value).apply()

    fun isRewardActive(): Boolean {
        return System.currentTimeMillis() < rewardExpiryMs
    }

    fun incrementRewardCounter() {
        val newCount = rewardCounter + 1
        rewardCounter = newCount
        totalAdsWatched = totalAdsWatched + 1
        if (newCount >= REWARD_THRESHOLD) {
            val currentExpiry = if (isRewardActive()) rewardExpiryMs else System.currentTimeMillis()
            rewardExpiryMs = currentExpiry + ONE_DAY_MS
            rewardCounter = 0
        }
    }

    // --- Admin User List ---
    fun getUserList(): List<Triple<String, String, Boolean>> {
        val raw = prefs.getString(KEY_ADMIN_USER_LIST, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size >= 3) {
                Triple(parts[0], parts[1], parts[2] == "true")
            } else null
        }
    }

    fun addOrUpdateUser(uid: String, email: String, adFree: Boolean) {
        val list = getUserList().toMutableList()
        val existingIndex = list.indexOfFirst { it.first == uid }
        if (existingIndex >= 0) {
            list[existingIndex] = Triple(uid, email, adFree)
        } else {
            list.add(Triple(uid, email, adFree))
        }
        saveUserList(list)
    }

    fun setUserAdFree(uid: String, adFree: Boolean) {
        val list = getUserList().toMutableList()
        val index = list.indexOfFirst { it.first == uid }
        if (index >= 0) {
            val user = list[index]
            list[index] = Triple(user.first, user.second, adFree)
            saveUserList(list)
        }
    }

    private fun saveUserList(list: List<Triple<String, String, Boolean>>) {
        val raw = list.joinToString(";") { "${it.first}:${it.second}:${it.third}" }
        prefs.edit().putString(KEY_ADMIN_USER_LIST, raw).apply()
    }

    // --- Ad Show Gate ---
    fun shouldShowAds(): Boolean {
        if (adminAdFreeOverride) return false
        // A subscription unlocks automation, but it does not silently remove ads.
        // Only an explicit admin override (synced by an admin action) is ad-free.
        return !isAdmin
    }

    // --- App Settings ---
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var customTexts: String
        get() = prefs.getString(KEY_CUSTOM_TEXTS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_TEXTS, value).apply()

    var targetPackage: String
        get() = prefs.getString(KEY_TARGET_PACKAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_PACKAGE, value).apply()

    var isRapidoMode: Boolean
        get() = prefs.getBoolean(KEY_RAPIDO_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_RAPIDO_MODE, value).apply()

    var isOlaMode: Boolean
        get() = prefs.getBoolean(KEY_OLA_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_OLA_MODE, value).apply()

    var isUberMode: Boolean
        get() = prefs.getBoolean(KEY_UBER_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_UBER_MODE, value).apply()

    var globalAdvancedMatching: Boolean
        get() = prefs.getBoolean(KEY_GLOBAL_ADVANCED_MATCHING, false)
        set(value) = prefs.edit().putBoolean(KEY_GLOBAL_ADVANCED_MATCHING, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_API_TOKEN)
            .remove(KEY_IS_ADMIN)
            .remove(KEY_REFERRAL_CODE)
            .remove(KEY_SUBSCRIPTION_UNTIL_MS)
            .remove(KEY_REWARD_COUNTER)
            .remove(KEY_TOTAL_ADS_WATCHED)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }
}
