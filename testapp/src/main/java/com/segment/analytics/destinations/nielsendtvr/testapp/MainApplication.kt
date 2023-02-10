package com.segment.analytics.destinations.nielsendtvr.testapp

import android.app.Application
import com.segment.analytics.kotlin.android.Analytics
import com.segment.analytics.kotlin.destinations.nielsendtvr.NielsenDTVRDestination

class MainApplication : Application() {
    companion object {
        lateinit var analyticsHandler: AnalyticsHandler
    }

    override fun onCreate() {
        super.onCreate()
        val analytics = Analytics(BuildConfig.SEGMENT_WRITE_KEY, applicationContext) {
            this.collectDeviceId = true
            this.trackApplicationLifecycleEvents = true
            this.trackDeepLinks = true
            this.flushAt = 1
            this.flushInterval = 0
        }
        analytics.add(NielsenDTVRDestination())
        analyticsHandler = AnalyticsHandler(analytics)
    }

    fun getAnalyticsHandler(): AnalyticsHandler {
        return analyticsHandler
    }
}
