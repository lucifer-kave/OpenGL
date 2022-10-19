package com.example.opengl

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.opengl.webscket.WebSocketService
import kotlinx.android.synthetic.main.activity_rec.*
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
        btn_record.setOnClickListener {
            mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent: Intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, SCREEN_CAPTURE_REQUEST_CODE)
            startForegroundService()
        }
        btn_start.setOnClickListener {
            mRECService?.startRecord()
        }
        checkPermission()
    }

    /**
     * 权限申请
     */
    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var permissions = arrayOf<String>(Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode === 200) {
            for ((i, permission) in permissions.withIndex()) {
                if (grantResults[i] !== PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, 200)
                    return
                }
            }
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