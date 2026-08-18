package com.mamabhutnika.rideaccepter

import android.app.Application
import android.util.Log
import com.startapp.sdk.adsbase.StartAppSDK

class MamaBhutnikaApp : Application() {

    companion object {
        const val TAG = "MamaBhutnikaApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Start.io SDK
        StartAppSDK.init(this, "207133232", true)
        StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true)

        Log.d(TAG, "Start.io SDK initialized with App ID: 207133232")
    }
}
