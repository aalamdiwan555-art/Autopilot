package com.autopilot.driver

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import com.startapp.sdk.adsbase.model.AdPreferences
import com.startapp.sdk.adsbase.model.Impression
import com.startapp.sdk.ads.video.RewardedVideo

/**
 * Keeps exactly one rewarded instance alive and waits for late inventory.
 * This avoids showing a blank screen when preload finishes just after a tap.
 */
object AdManager {
    private const val TAG = "StartIO"
    private const val RETRY_MS = 30_000L
    private const val WAIT_MS = 60_000L
    private const val POLL_MS = 500L
    private val handler = Handler(Looper.getMainLooper())
    private var rewardedAd: RewardedVideo? = null
    private var loading = false

    @Synchronized
    fun loadRewardedAd(context: Context) {
        if (loading || rewardedAd?.isReady == true) return
        loading = true
        try {
            val ad = RewardedVideo(context.applicationContext)
            rewardedAd = ad
            ad.load(object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    loading = false
                    Log.d(TAG, "Ad Loaded")
                }
                override fun onFailedToReceiveAd(ad: Ad) {
                    loading = false
                    rewardedAd = null
                    Log.e(TAG, "Failed to load rewarded ad")
                    handler.postDelayed({ loadRewardedAd(context.applicationContext) }, RETRY_MS)
                }
                override fun onClick(ad: Ad) = Unit
                override fun onImpression(ad: Ad) = Unit
            })
        } catch (error: Exception) {
            loading = false
            rewardedAd = null
            Log.e(TAG, "Start.io preload crashed", error)
            handler.postDelayed({ loadRewardedAd(context.applicationContext) }, RETRY_MS)
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onCompleted: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val startedAt = System.currentTimeMillis()
        fun poll() {
            val ad = rewardedAd
            if (ad?.isReady == true) {
                try {
                    ad.show(object : AdEventListener {
                        override fun onReceiveAd(ad: Ad) = Unit
                        override fun onFailedToReceiveAd(ad: Ad) {
                            Toast.makeText(activity, "No ads available. Try again", Toast.LENGTH_LONG).show()
                            onUnavailable()
                            loadRewardedAd(activity.applicationContext)
                        }
                        override fun onClick(ad: Ad) = Unit
                        override fun onImpression(ad: Ad) = Unit
                    }, object : VideoListener {
                        override fun onVideoCompleted() {
                            onCompleted()
                            rewardedAd = null
                            loadRewardedAd(activity.applicationContext)
                        }
                    })
                } catch (error: Exception) {
                    Log.e(TAG, "Could not show rewarded ad", error)
                    onUnavailable()
                    loadRewardedAd(activity.applicationContext)
                }
                return
            }
            if (System.currentTimeMillis() - startedAt >= WAIT_MS) {
                Toast.makeText(activity, "No ads available. Try again", Toast.LENGTH_LONG).show()
                onUnavailable()
                loadRewardedAd(activity.applicationContext)
                return
            }
            loadRewardedAd(activity.applicationContext)
            handler.postDelayed(::poll, POLL_MS)
        }
        Toast.makeText(activity, "Loading Ad...", Toast.LENGTH_SHORT).show()
        poll()
    }
}