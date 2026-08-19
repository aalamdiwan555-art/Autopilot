package com.autopilot.driver

import android.app.Application
import com.startapp.sdk.adsbase.StartAppSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        require(BuildConfig.STARTIO_APP_ID.isNotBlank()) {
            "STARTIO_APP_ID must be configured for rewarded ads"
        }
        StartAppSDK.init(this, BuildConfig.STARTIO_APP_ID, false)
        StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true)
        if (BuildConfig.STARTIO_TEST_MODE) StartAppSDK.setTestAdsEnabled(true)
        AdManager.loadRewardedAd(this)
    }
}
