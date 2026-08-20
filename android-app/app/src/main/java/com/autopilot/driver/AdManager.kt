package com.autopilot.driver

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import com.startapp.sdk.adsbase.model.AdPreferences

/**
 * Single ad gateway for the active Autopilot app.
 *
 * Ads are shown only while the user is in the app. Accessibility, OCR and
 * foreground-service flows never invoke this class, which prevents an
 * unexpected ad from interrupting a ride decision.
 */
object AdManager {
    private const val TAG = "AutopilotAds"
    private const val INTERSTITIAL_COOLDOWN_MS = 45_000L
    private const val RETRY_DELAY_MS = 30_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var interstitial: StartAppAd? = null
    private var interstitialReady = false
    private var lastInterstitialAt = 0L
    private var retryScheduled = false

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        preloadInterstitial()
    }

    fun attachBanner(parent: ViewGroup): Banner? {
        val context = appContext ?: parent.context.applicationContext
        if (appContext == null) initialize(context)
        return try {
            parent.removeAllViews()
            Banner(context).also { banner ->
                parent.addView(
                    banner,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        } catch (error: Exception) {
            Log.w(TAG, "Banner could not be attached", error)
            null
        }
    }

    fun showInterstitial(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val now = System.currentTimeMillis()
        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) return
        try {
            val ad = interstitial
            if (interstitialReady && ad != null) {
                interstitialReady = false
                lastInterstitialAt = now
                ad.showAd()
                preloadInterstitial()
            } else {
                preloadInterstitial()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Interstitial could not be shown", error)
            preloadInterstitial()
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onFailed: () -> Unit,
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFailed()
            return
        }
        try {
            val rewarded = StartAppAd(activity)
            rewarded.setVideoListener(object : VideoListener {
                override fun onVideoCompleted() {
                    onRewarded()
                }
            })
            rewarded.loadAd(
                StartAppAd.AdMode.REWARDED_VIDEO,
                object : AdEventListener {
                    override fun onReceiveAd(ad: Ad) {
                        rewarded.showAd()
                    }

                    override fun onFailedToReceiveAd(ad: Ad?) {
                        Toast.makeText(activity, "Ad unavailable. Try again shortly.", Toast.LENGTH_SHORT).show()
                        onFailed()
                    }
                },
            )
        } catch (error: Exception) {
            Log.w(TAG, "Rewarded ad failed", error)
            onFailed()
        }
    }

    private fun preloadInterstitial() {
        val context = appContext ?: return
        if (interstitialReady || retryScheduled) return
        try {
            val ad = StartAppAd(context)
            interstitial = ad
            ad.loadAd(
                AdPreferences(),
                object : AdEventListener {
                    override fun onReceiveAd(received: Ad) {
                        interstitialReady = true
                        retryScheduled = false
                    }

                    override fun onFailedToReceiveAd(failed: Ad?) {
                        interstitialReady = false
                        scheduleRetry()
                    }
                },
            )
        } catch (error: Exception) {
            Log.w(TAG, "Interstitial preload failed", error)
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (retryScheduled) return
        retryScheduled = true
        mainHandler.postDelayed({
            retryScheduled = false
            preloadInterstitial()
        }, RETRY_DELAY_MS)
    }
}