package com.segment.analytics.kotlin.destinations.nielsendtvr

import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin

class NielsenDTVRDestination : DestinationPlugin() {
    override val key: String = "Nielsen DTVR"

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        if (type == Plugin.UpdateType.Initial) {
        }
    }

    override fun track(payload: TrackEvent): BaseEvent? {
        return super.track(payload)
    }
}