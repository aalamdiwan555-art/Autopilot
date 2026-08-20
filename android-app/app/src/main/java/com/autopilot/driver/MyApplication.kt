package com.autopilot.driver

import android.app.Application
import com.startapp.sdk.adsbase.StartAppSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StartAppSDK.init(this, BuildConfig.STARTIO_APP_ID, true)
        StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true)
        // Production builds serve live Start.io inventory. Test mode is opt-in
        // only for local QA via -PSTARTIO_TEST_MODE=true.
        StartAppSDK.setTestAdsEnabled(BuildConfig.STARTIO_TEST_MODE)
        AdManager.initialize(this)
    }
}
