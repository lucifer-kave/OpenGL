package com.example.opengl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.opengl.webscket.WebSocketService
import okio.ByteString


class RECActivity : AppCompatActivity() {
    private val SCREEN_CAPTURE_REQUEST_CODE = 1
    private lateinit var mediaProjection: MediaProjection
    private lateinit var mediaProjectionManager:MediaProjectionManager
    private var isBind = false
    private var mWebSocketService:WebSocketService? = null
    private var mRECService:RECService? = null
    private val connection:ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBind = true
            val myBinder = service as WebSocketService.WebSocketBinder
            mWebSocketService = myBinder.service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBind = false
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rec)
        findViewById<Button>(R.id.btn_record).setOnClickListener {
            mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent: Intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, SCREEN_CAPTURE_REQUEST_CODE)
            startForegroundService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            mRECService?.mediaProjection = mediaProjection
        }
    }

    fun startForegroundService() {
        intent = Intent(this, RECService::class.java)
        intent.putExtra("from", "RECActivity");
        bindService(intent, object : ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                var binder = service as RECService.RECBinder
                mRECService = binder.service
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                TODO("Not yet implemented")
            }
        },Context.BIND_AUTO_CREATE)
    }

    fun bindWebSocketService() {
        intent = Intent(this, WebSocketService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun sendData(byteString: ByteString) {
        mWebSocketService?.sendData(byteString)
    }
}