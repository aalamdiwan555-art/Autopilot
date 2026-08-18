package com.mamabhutnika.rideaccepter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import com.startapp.sdk.adsbase.model.AdPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdManager private constructor(private val context: Context) {

    companion object {
        const val TAG = "AdManager"
        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = UserPrefs(context)
    private val api = ApiClient()
    private val startAppAd = StartAppAd(context)
    private var isInterstitialReady = false
    private var isRewardedReady = false
    private var currentBanner: Banner? = null

    init {
        preloadInterstitial()
        preloadRewarded()
    }

    // ========================================================
    // EXCLUSION CHECK: Central gatekeeper
    // ========================================================
    fun canShowAds(): Boolean {
        val canShow = prefs.shouldShowAds()
        Log.d(TAG, "canShowAds() = $canShow | adminOverride=${prefs.adminAdFreeOverride} | rewardActive=${prefs.isRewardActive()} | inTrial=${prefs.isInTrialPeriod()} | isAdFree=${prefs.isAdFree}")
        return canShow
    }

    // ========================================================
    // BANNER AD
    // ========================================================
    fun attachBanner(parent: ViewGroup) {
        if (!canShowAds()) {
            Log.d(TAG, "Banner suppressed: user is ad-free")
            detachBanner(parent)
            return
        }
        try {
            currentBanner?.let { parent.removeView(it) }
            val banner = Banner(context)
            parent.addView(banner)
            currentBanner = banner
            Log.d(TAG, "Banner attached")
        } catch (e: Exception) {
            Log.e(TAG, "Banner attach failed", e)
        }
    }

    fun detachBanner(parent: ViewGroup) {
        try {
            currentBanner?.let { parent.removeView(it) }
            currentBanner = null
        } catch (_: Exception) {}
    }

    // ========================================================
    // INTERSTITIAL AD
    // ========================================================
    private fun preloadInterstitial() {
        if (!canShowAds()) return
        startAppAd.loadAd(AdPreferences().apply {
            // Standard interstitial
        }, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                isInterstitialReady = true
                Log.d(TAG, "Interstitial preloaded")
            }
            override fun onFailedToReceiveAd(ad: Ad?) {
                isInterstitialReady = false
                Log.w(TAG, "Interstitial preload failed")
            }
        })
    }

    fun showInterstitial(activity: Activity) {
        if (!canShowAds()) {
            Log.d(TAG, "Interstitial suppressed: user is ad-free")
            return
        }
        try {
            if (isInterstitialReady) {
                isInterstitialReady = !startAppAd.showAd()
                preloadInterstitial()
            } else {
                StartAppAd.showAd(activity)
                preloadInterstitial()
            }
            Log.d(TAG, "Interstitial shown")
        } catch (e: Exception) {
            Log.e(TAG, "Interstitial error", e)
        }
    }

    fun showExitInterstitial(activity: Activity) {
        if (!canShowAds()) return
        try {
            StartAppAd.onBackPressed(activity)
            Log.d(TAG, "Exit interstitial triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Exit interstitial error", e)
        }
    }

    // ========================================================
    // REWARDED VIDEO AD (10-watch system)
    // ========================================================
    private fun preloadRewarded() {
        if (!canShowAds()) return
        try {
            val rewardedAd = StartAppAd(context)
            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isRewardedReady = true
                    Log.d(TAG, "Rewarded video preloaded")
                }
                override fun onFailedToReceiveAd(ad: Ad?) {
                    isRewardedReady = false
                    Log.w(TAG, "Rewarded preload failed")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded preload error", e)
        }
    }

    fun showRewardedVideo(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        if (!canShowAds()) {
            Toast.makeText(context, "You are already ad-free!", Toast.LENGTH_SHORT).show()
            onFailed()
            return
        }
        try {
            val rewardedAd = StartAppAd(context)
            rewardedAd.setVideoListener(object : VideoListener {
                override fun onVideoCompleted() {
                    Log.d(TAG, "Rewarded video COMPLETED")
                    val token = prefs.apiToken
                    if (token.isBlank()) {
                        Toast.makeText(context, "Please sign in again before watching ads.", Toast.LENGTH_LONG).show()
                        onFailed()
                        return
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val updated = api.recordAdCompleted(token)
                            prefs.applyRemoteUser(updated)
                            withContext(Dispatchers.Main) {
                                val message = if (updated.rewardProgress == 0) {
                                    "You earned 1 free subscription day!"
                                } else {
                                    "Ad watched! Progress: ${updated.rewardProgress} / ${UserPrefs.REWARD_THRESHOLD}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                onRewarded()
                            }
                        } catch (error: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    error.message ?: "Could not apply the ad reward.",
                                    Toast.LENGTH_LONG,
                                ).show()
                                onFailed()
                            }
                        } finally {
                            preloadRewarded()
                        }
                    }
                }
            })

            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    rewardedAd.showAd()
                }
                override fun onFailedToReceiveAd(ad: Ad?) {
                    Toast.makeText(context, "Ad failed to load. Try again.", Toast.LENGTH_SHORT).show()
                    onFailed()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show error", e)
            onFailed()
        }
    }

    fun getRewardProgress(): String {
        return "${prefs.rewardCounter} / ${UserPrefs.REWARD_THRESHOLD}"
    }

    fun getRewardStatusText(): String {
        return when {
            prefs.adminAdFreeOverride -> "Ad-Free: ADMIN OVERRIDE"
            prefs.hasActiveSubscription() -> {
                val remaining = (prefs.subscriptionUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
                "Subscription active: ${remaining / UserPrefs.ONE_DAY_MS}d ${(remaining % UserPrefs.ONE_DAY_MS) / 3_600_000L}h remaining"
            }
            else -> "Subscription expired - watch 10 videos for 1 free day"
        }
    }
}
