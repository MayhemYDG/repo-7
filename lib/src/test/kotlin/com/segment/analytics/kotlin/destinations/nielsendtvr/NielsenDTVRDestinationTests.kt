package com.segment.analytics.kotlin.destinations.nielsendtvr

import android.app.Application
import android.content.Context
import com.nielsen.app.sdk.AppSdk
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.emptyJsonObject
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.utilities.LenientJson
import com.segment.analytics.kotlin.destinations.matchers.matchJSONObject
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NielsenDTVRDestinationTests {
    @MockK
    lateinit var mockApplication: Application
    @MockK
    lateinit var mockedContext: Context
    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics
    @MockK(relaxUnitFun = true)
    lateinit var mockedAppSdk: AppSdk

    lateinit var mockedNielsenDTVRDestination: NielsenDTVRDestination
    private val sampleNielsenNCRSettings: Settings = LenientJson.decodeFromString(
        """
            {
              "integrations": {
                "Nielsen DTVR": {
                   "appId": "APPID1234567890",
                   "sfCode": true,
                   "debug": true,
                   "sendId3Events": [
                      "sendId3EventA",
                      "sendId3EventB"
                   ],
                   "id3Property" : "id3"
                }
              }
            }
        """.trimIndent()
    )

    init {
        MockKAnnotations.init(this)
    }

    @Before
    fun setUp() {
        mockedNielsenDTVRDestination = NielsenDTVRDestination()
        every { mockedAnalytics.configuration.application } returns mockApplication
        every { mockApplication.applicationContext } returns mockedContext
        mockedAnalytics.configuration.application = mockedContext
        mockedNielsenDTVRDestination.analytics = mockedAnalytics

        // An Nielsen DTVR example settings
        val nielsenNCRSettings: Settings = sampleNielsenNCRSettings
        mockedNielsenDTVRDestination.update(nielsenNCRSettings, Plugin.UpdateType.Initial)
        mockedNielsenDTVRDestination.appSdk = mockedAppSdk
    }

    @Test
    fun `settings are updated correctly`() {

        /* assertions Nielsen DTVR config */
        Assertions.assertNotNull(mockedNielsenDTVRDestination.nielsenDTVRSettings)
        with(mockedNielsenDTVRDestination.nielsenDTVRSettings!!) {
            assertEquals(appId, "APPID1234567890")
            assertEquals(sfCode, "true")
            assertEquals(debug, true)
        }
    }

    @Test
    fun `track for Video Content Started handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Content Started",
            properties = buildJsonObject {
                put("load_type", "linear")
                put("channel", "Channel A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }

        mockedNielsenDTVRDestination.track(sampleEvent)
        val expectedChannelInfo = JSONObject()
        expectedChannelInfo.put("channelName", "Channel A")

        val expectedMetaData = JSONObject()
        expectedMetaData.put("adModel", "1")
        expectedMetaData.put("channelName", "Channel A")
        expectedMetaData.put("type", "content")
        verify { mockedAppSdk.loadMetadata(matchJSONObject(expectedMetaData)) }
        verify { mockedAppSdk.play(matchJSONObject(expectedChannelInfo)) }
    }

    @Test
    fun `track for Video Playback Resumed handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Resumed",
            properties = buildJsonObject {
                put("channel", "Channel A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        val expectedChannelInfo = JSONObject()
        expectedChannelInfo.put("channelName", "Channel A")

        verify { mockedAppSdk.play(matchJSONObject(expectedChannelInfo)) }
    }

    @Test
    fun `track for Video Playback Seek Completed handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Seek Completed",
            properties = buildJsonObject {
                put("channel", "Channel A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        val expectedChannelInfo = JSONObject()
        expectedChannelInfo.put("channelName", "Channel A")

        verify { mockedAppSdk.play(matchJSONObject(expectedChannelInfo)) }
    }

    @Test
    fun `track for Video Playback Buffer Completed handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Buffer Completed",
            properties = buildJsonObject {
                put("channel", "Channel B")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        val expectedChannelInfo = JSONObject()
        expectedChannelInfo.put("channelName", "Channel B")

        verify { mockedAppSdk.play(matchJSONObject(expectedChannelInfo)) }
    }

    @Test
    fun `track for Video Playback Paused handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Paused",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Video Playback Interrupted handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Interrupted",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }
    @Test
    fun `track for Video Content Completed handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Content Completed",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Video Playback Buffer Started handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Buffer Started",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Video Playback Seek Started handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Seek Started",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Video Video Playback Exited handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Exited",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Video Video Playback Completed handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Completed",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for Application Backgrounded handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "Application Backgrounded",
            properties = emptyJsonObject
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.stop() }
    }

    @Test
    fun `track for ID3 event handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "sendId3EventA",
            properties = buildJsonObject {
                put("id3", "Test ID 3 A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify { mockedAppSdk.sendID3("Test ID 3 A") }
    }

    @Test
    fun `track for ID3 multiple event handled correctly`() {
        val sampleEventA = TrackEvent(
            event = "sendId3EventA",
            properties = buildJsonObject {
                put("id3", "Test ID 3 A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        val sampleEventB = TrackEvent(
            event = "sendID3EventA",
            properties = buildJsonObject {
                put("id3", "Test ID 3 B")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }

        mockedNielsenDTVRDestination.track(sampleEventA)
        mockedNielsenDTVRDestination.track(sampleEventA)
        verify(exactly = 1) { mockedAppSdk.sendID3("Test ID 3 A") }

        mockedNielsenDTVRDestination.track(sampleEventB)
        verify{ mockedAppSdk.sendID3("Test ID 3 B") }
        verify(exactly = 2){mockedAppSdk.sendID3(any())}
    }

    @Test
    fun `track for non ID3 event handled correctly`() {
        val sampleEvent = TrackEvent(
            event = "nonID3Event",
            properties = buildJsonObject {
                put("id3", "Test Non ID 3 A")
            }
        ).apply {
            anonymousId = "anonymous_UserID-123"
            integrations = emptyJsonObject
            context = emptyJsonObject
        }
        mockedNielsenDTVRDestination.track(sampleEvent)
        verify(exactly = 0) { mockedAppSdk.sendID3(any()) }
    }
}