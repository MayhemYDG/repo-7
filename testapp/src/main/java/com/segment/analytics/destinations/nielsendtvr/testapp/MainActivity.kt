package com.segment.analytics.destinations.nielsendtvr.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.bt_play_video).setOnClickListener {
            startActivity(Intent(this@MainActivity, VideoActivity::class.java))
        }
    }
}