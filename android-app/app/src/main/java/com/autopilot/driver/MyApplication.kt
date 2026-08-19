package com.autopilot.driver

import android.app.Application
import com.startapp.sdk.adsbase.StartAppSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StartAppSDK.init(this, BuildConfig.STARTIO_APP_ID, true)
        StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true)
        if (BuildConfig.STARTIO_TEST_MODE) StartAppSDK.setTestAdsEnabled(true)
        AdManager.initialize(this)
    }
}
