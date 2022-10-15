package com.example.opengl

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class RECActivity : AppCompatActivity() {
    private val SCREEN_CAPTURE_REQUEST_CODE = 1
    private lateinit var mediaProjection: MediaProjection
    private lateinit var mediaProjectionManager:MediaProjectionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        findViewById<Button>(R.id.btn_record).setOnClickListener {
            mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent: Intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, SCREEN_CAPTURE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        }
    }

    fun startF
}