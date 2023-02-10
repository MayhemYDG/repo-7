package com.segment.analytics.destinations.nielsendtvr.testapp

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

/**
 * A Custom Video View to capture and control all video events by invoking Analytics specs.
 */
class AnalyticsVideoView : VideoView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    private var isResuming: Boolean= false
    private var analyticsHandler: AnalyticsHandler? = null

    override fun start() {
        if (isResuming) {
            analyticsHandler?.trackPlaybackResumed()
        } else {
            analyticsHandler?.trackContentStart()
            isResuming = true
        }
        super.start()
    }

    override fun pause() {
        analyticsHandler?.trackPlaybackPaused()
        super.pause()
    }

    override fun seekTo(msec: Int) {
        analyticsHandler?.trackSeekStarted()
        super.seekTo(msec)
    }

    fun setAnalytics(analyticsHandler: AnalyticsHandler) {
        this.analyticsHandler = analyticsHandler
    }

    fun clearState() {
        isResuming = false
    }

    fun setResuming() {
        isResuming = true
    }
    fun isResuming(): Boolean {
        return isResuming
    }
}
