package com.segment.analytics.destinations.nielsendtvr.testapp

import com.segment.analytics.kotlin.core.Analytics
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Handler class to handle and call all Analytics track calls.
 */
class AnalyticsHandler(private val analytics: Analytics) {
    private fun trackEvent(event: String) {
        analytics.track(
            event,
            properties = buildJsonObject {
                put("channel", "A")
            }
        )
    }

    fun trackContentStart() {
        analytics.track(
            "Video Content Started",
            properties = buildJsonObject {
                put("load_type", "linear")
                put("channel", "A")
            }
        )
    }

    fun trackPlaybackResumed() {
        trackEvent("Video Playback Resumed")
    }

    fun trackSeekCompleted() {
        trackEvent("Video Playback Seek Completed")
    }

    fun trackBufferCompleted() {
        trackEvent("Video Playback Buffer Completed")
    }

    fun trackPlaybackPaused() {
        analytics.track("Video Playback Paused")
    }

    fun trackContentCompleted() {
        analytics.track("Video Content Completed")
    }

    fun trackBufferStarted() {
        analytics.track("Video Playback Buffer Started")
    }

    fun trackSeekStarted() {
        analytics.track("Video Playback Seek Started")
    }

    fun trackApplicationBackgrounded() {
        analytics.track("Application Backgrounded")
    }

    fun trackPlaybackCompleted() {
        analytics.track("Video Playback Completed")
    }

    fun trackID3Event(id3: String) {
        analytics.track(
            "sendID3",
            properties = buildJsonObject {
                put("Id3", id3)
            }
        )
    }
}
