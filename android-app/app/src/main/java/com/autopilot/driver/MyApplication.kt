package com.autopilot.driver

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError

class MyApplication : Application() {
    @Volatile var rewardedAd: RewardedAd? = null
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        preloadRewarded()
    }
    fun preloadRewarded() {
        RewardedAd.load(this, getString(R.string.admob_rewarded_id), AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            })
    }
}