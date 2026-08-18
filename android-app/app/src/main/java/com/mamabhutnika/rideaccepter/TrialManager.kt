package com.mamabhutnika.rideaccepter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TrialManager(private val context: Context) {

    companion object {
        const val TAG = "TrialManager"
        const val WORLD_TIME_API = "https://worldtimeapi.org/api/ip"
        const val FALLBACK_MAX_DRIFT_MS = 300_000L // 5 min tolerance
    }

    private val prefs = UserPrefs(context)

    suspend fun verifyAndStartTrial(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val networkTimeMs = fetchNetworkTime()
                if (networkTimeMs > 0) {
                    val localTime = System.currentTimeMillis()
                    val drift = kotlin.math.abs(localTime - networkTimeMs)

                    if (drift > FALLBACK_MAX_DRIFT_MS) {
                        Log.w(TAG, "Time drift detected: ${drift}ms. Using network time.")
                        // Tamper detected - use network time as baseline
                        prefs.trialStartMs = networkTimeMs
                    } else {
                        // Local time is trustworthy
                        if (prefs.trialStartMs == 0L) {
                            prefs.trialStartMs = localTime
                        }
                    }
                    true
                } else {
                    // Network unavailable - fall back to local with tamper check
                    if (prefs.trialStartMs == 0L) {
                        prefs.trialStartMs = System.currentTimeMillis()
                    }
                    Log.w(TAG, "Network time unavailable. Using local time with tamper guard.")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Trial verification failed", e)
                if (prefs.trialStartMs == 0L) {
                    prefs.trialStartMs = System.currentTimeMillis()
                }
                true
            }
        }
    }

    private fun fetchNetworkTime(): Long {
        return try {
            val connection = URL(WORLD_TIME_API).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.getInputStream().bufferedReader().use { it.readText() }

            // Parse ISO 8601 datetime from JSON response
            val datetimeRegex = """"datetime":"([^"]+)"""".toRegex()
            val match = datetimeRegex.find(response)
            val datetimeStr = match?.groupValues?.get(1) ?: return 0L

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(datetimeStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Network time fetch failed", e)
            0L
        }
    }

    fun getTrialStatus(): String {
        return when {
            prefs.trialStartMs == 0L -> "Trial not started"
            prefs.isInTrialPeriod() -> {
                val remainingMin = prefs.getTrialRemainingMs() / 60000
                "Trial active: $remainingMin min remaining"
            }
            else -> "Trial expired"
        }
    }
}
