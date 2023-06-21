//
// MIT License
//
// Copyright (c) 2023 Segment
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.segment.analytics.kotlin.destinations.nielsendtvr

import android.content.Context
import com.nielsen.app.sdk.AppSdk
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.toContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class NielsenDTVRDestination : DestinationPlugin() {
    companion object {
        private const val NIELSEN_DTVR_FULL_KEY = "Nielsen DTVR"
    }

    private var previousID3: String = ""
    internal var nielsenDTVRSettings: NielsenDTVRSettings? = null
    internal lateinit var appSdk: AppSdk
    internal var id3EventNames: ArrayList<String> = arrayListOf()
    private var id3PropertyName: String = ""

    override val key: String = NIELSEN_DTVR_FULL_KEY

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        this.nielsenDTVRSettings =
            settings.destinationSettings(key, NielsenDTVRSettings.serializer())
        if (type == Plugin.UpdateType.Initial) {
            if (nielsenDTVRSettings != null) {
                setupNielsenAppSdk()
               id3EventNames =  parseId3EventNames()
                id3PropertyName = nielsenDTVRSettings!!.id3Property.ifEmpty { "id3" }
            }
        }
    }

    override fun track(payload: TrackEvent): BaseEvent {
        val trackEnum: EventVideoEnum? = EventVideoEnum[payload.event]
        val nielsenProperties: Map<String, String> = payload.properties.asStringMap()
        if(trackEnum!=null) {
            when (trackEnum) {
                EventVideoEnum.ContentStarted -> {
                    play(nielsenProperties)
                    loadMetadata(nielsenProperties)
                }
                EventVideoEnum.PlaybackResumed,
                EventVideoEnum.PlaybackSeekCompleted,
                EventVideoEnum.PlaybackBufferCompleted -> {
                    play(nielsenProperties)
                }
                EventVideoEnum.PlaybackPaused,
                EventVideoEnum.PlaybackInterrupted,
                EventVideoEnum.ContentCompleted,
                EventVideoEnum.PlaybackBufferStarted,
                EventVideoEnum.PlaybackSeekStarted,
//            Nielsen requested Video Playback Completed and new Video Playback Exited event map to stop as end is not used for DTVR
                EventVideoEnum.PlaybackExited,
                EventVideoEnum.PlaybackCompleted -> {
                    stop()
                }
                EventVideoEnum.ApplicationBackgrounded -> {
                    stop()
                }
                else -> {
                    analytics.log("Video Event not found")
                }
            }
        }
        if (id3EventNames.contains(payload.event.lowercase(Locale.getDefault()))) {
            sendID3(nielsenProperties)
        }
        return payload
    }

    /**
     * creating Nielsen App SDK.
     */
    private fun setupNielsenAppSdk() {
        var sfcode = "us"
        if (!nielsenDTVRSettings!!.sfCode.isNullOrEmpty()) {
            sfcode = nielsenDTVRSettings!!.sfCode!!
        }
        // Create AppSdk configuration object (JSONObject)
        val appSdkConfig: JSONObject = JSONObject()
            .put("appid", nielsenDTVRSettings!!.appId)
            .put("sfcode", sfcode)
        if (nielsenDTVRSettings!!.debug) {
            appSdkConfig.put("nol_devDebug", "DEBUG")
        }
        appSdk = AppSdk(analytics.configuration.application as Context, appSdkConfig, null)
        analytics.log("new AppSdk(${appSdkConfig.toString(2)})")
    }

    /**
     * retrieves lowercase list of id3 event names from settings
     *
     * @return list of lower case id3 event names
     */
    private fun parseId3EventNames(): ArrayList<String> {
        val id3EventNames = ArrayList<String>()
        id3EventNames.addAll(nielsenDTVRSettings!!.sendId3Events)
        for (i in id3EventNames.indices) {
            id3EventNames[i] = id3EventNames[i].lowercase(Locale.getDefault())
        }
        return id3EventNames
    }

    /**
     * Creating Metadata in JSONObject type. JSON value must be string value. And send it to neilsen SDK
     * @param nielsenProperties Map<String, String> properties from payload of the Segment track event
     */
    private fun loadMetadata(nielsenProperties: Map<String, String>) {
        val jsonMetadata = JSONObject()
        try {
            jsonMetadata.put("type", "content")
            if (nielsenProperties.containsKey("channel")) {
                jsonMetadata.put("channelName", nielsenProperties["channel"])
            }
            var loadType = ""
            if (nielsenProperties.containsKey("load_type")) {
                loadType = "load_type"
            }
            if (nielsenProperties.containsKey("loadType")) {
                loadType = "loadType"
            }
            if (loadType.isNotEmpty()) {
                jsonMetadata.put(
                    "adModel",
                    if (nielsenProperties[loadType].equals("dynamic")) "2" else "1"
                )
            }
        } catch (e: JSONException) {
            analytics.log("Failed to send loadMetadata event : $e")
        }
        analytics.log("appSdk.loadMetadata($jsonMetadata)")
        appSdk.loadMetadata(jsonMetadata)
    }

    /**
     * a method to sendID3 value to NeilsenSDK
     * @param nielsenProperties Map<String, String> properties from payload of the Segment track event
     */
    private fun sendID3(nielsenProperties: Map<String, String>) {
        val id3: String? = nielsenProperties[id3PropertyName]
        if (id3.isNullOrEmpty() || previousID3 == id3)
            return
        previousID3 = id3
        analytics.log("appSdk.sendID3$id3)")
        appSdk.sendID3(id3)
    }

    private fun play(nielsenProperties: Map<String, String>) {
        val channelInfo = JSONObject()
        try {
            if (nielsenProperties.containsKey("channel")) {
                channelInfo.put("channelName", nielsenProperties["channel"])
            }
        } catch (e: JSONException) {
            analytics.log( "Failed to send play event : $e")
        }
        analytics.log("appSdk.play($channelInfo)")
        appSdk.play(channelInfo)
    }

    private fun stop() {
        analytics.log("appSdk.stop()")
        appSdk.stop()
    }
}

internal enum class EventVideoEnum(
    /**
     * Retrieves the Neilsen DTVR video event name. This is different from `enum.name()`
     *
     * @return Event name.
     */
    val eventName: String
) {
    PlaybackPaused("Video Playback Paused"),
    PlaybackResumed("Video Playback Resumed"),
    PlaybackExited("Video Playback Exited"),
    PlaybackInterrupted("Video Playback Interrupted"),
    PlaybackCompleted("Video Playback Completed"),
    ContentStarted("Video Content Started"),
    ContentCompleted("Video Content Completed"),
    PlaybackBufferStarted("Video Playback Buffer Started"),
    PlaybackBufferCompleted("Video Playback Buffer Completed"),
    PlaybackSeekStarted("Video Playback Seek Started"),
    PlaybackSeekCompleted("Video Playback Seek Completed"),
    ApplicationBackgrounded("Application Backgrounded");

    companion object {
        private var names: MutableMap<String, EventVideoEnum>? = null

        init {
            names = HashMap()
            for (e in values()) {
                (names as HashMap<String, EventVideoEnum>)[e.eventName] = e
            }
        }

        operator fun get(name: String): EventVideoEnum? {
            if (names!!.containsKey(name)) {
                return names!![name]
            }
            return null
        }
        /**
         * Identifies if the event is a video event.
         *
         * @param eventName Event name
         * @return `true` if it's a video event, `false` otherwise.
         */
        fun isVideoEvent(eventName: String): Boolean {
            return names!!.containsKey(eventName)
        }
    }
}
/**
 * NielsenDTVR Settings data class.
 */
@Serializable
internal data class NielsenDTVRSettings(
    var appId: String,
    var debug: Boolean,
    var sfCode: String?,
    var sendId3Events: ArrayList<String> = arrayListOf(),
    var id3Property: String = "id3")

private fun JsonObject.asStringMap(): Map<String, String> = this.mapValues { (_, value) ->
    value.toContent().toString()
}
