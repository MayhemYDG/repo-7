package com.segment.analytics.destinations.nielsendtvr.testapp

import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.media.TimedMetaData
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import java.nio.charset.StandardCharsets

class VideoActivity : AppCompatActivity(), OnPreparedListener, OnCompletionListener,
    OnInfoListener, OnTimedMetaDataAvailableListener, OnSeekCompleteListener {
    private lateinit var analyticsHandler: AnalyticsHandler
    private lateinit var videoView: AnalyticsVideoView
    private var savedProgress = 0
    private var isPlaying = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        initAnalytics()
        initVideoView()
    }

    private fun initAnalytics() {
        videoView = findViewById(R.id.cv_video)
        analyticsHandler = (application as MainApplication).getAnalyticsHandler()
        videoView.setAnalytics(analyticsHandler)
    }

    private fun initVideoView() {
        val sampleVideoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        videoView.setOnPreparedListener(this)
        videoView.setOnCompletionListener(this)
        videoView.setOnInfoListener(this)
        videoView.setVideoURI(Uri.parse(sampleVideoUrl))
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
    }

    private fun saveProgress() {
        if (isFinishing) {
            analyticsHandler.trackPlaybackCompleted()
        } else {
            savedProgress = videoView.currentPosition
            isPlaying = videoView.isPlaying
            analyticsHandler.trackApplicationBackgrounded()
        }
    }

    private fun resumeProgress(mp: MediaPlayer) {
        if (savedProgress > 0) {
            videoView.seekTo(savedProgress)
        }
        if (isPlaying) {
            if (videoView.isResuming()) {
                analyticsHandler.trackPlaybackResumed()
            } else {
                analyticsHandler.trackContentStart()
                videoView.setResuming()
             }
            mp.start()
        }
    }
    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.setOnSeekCompleteListener(this)
        mp.setOnTimedMetaDataAvailableListener(this)
        resumeProgress(mp)
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        when (what) {
            MEDIA_INFO_BUFFERING_START -> analyticsHandler.trackBufferStarted()
            MEDIA_INFO_BUFFERING_END -> analyticsHandler.trackBufferCompleted()
        }
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        analyticsHandler.trackContentCompleted()
        analyticsHandler.trackPlaybackCompleted()
        videoView.clearState()
    }

    override fun onSeekComplete(mp: MediaPlayer?) {
        analyticsHandler.trackSeekCompleted()
    }

    override fun onTimedMetaDataAvailable(mp: MediaPlayer, data: TimedMetaData?) {
        if (data != null && mp.isPlaying) {
            val metadata = data.metaData
            if (metadata != null) {
                val iD3Payload = String(metadata, StandardCharsets.UTF_8)
                val index = iD3Payload.indexOf("www.nielsen.com")
                if (index != -1) {
                    val id3String = iD3Payload.substring(index, index + 249)
                    analyticsHandler.trackID3Event(id3String)
                }
            }
        }
    }
}