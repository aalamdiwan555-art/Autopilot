package com.autopilot.driver

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {
    private var mInterstitialAd: InterstitialAd? = null

    fun loadAd(adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                Log.d("AdManager", "Ad loaded")
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdManager", "Ad failed to load: ${adError.message}")
            }
        })
    }
}
