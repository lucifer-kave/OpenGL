package com.example.opengl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.opengl.Jni.render
import com.example.opengl.Jni.render29
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val PICK_VIDEO_RESULT_CODE = 101;
    private lateinit var surfaceView: SurfaceView
    private lateinit var glSurfaceView: VideoGLSurfaceView
    private lateinit var surface: RelativeLayout
    private lateinit var button: Button
    private lateinit var button2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surface = findViewById(R.id.surface_container)
        surfaceView = SurfaceView(this)
        surface.addView(surfaceView)
        button = findViewById(R.id.play)
        button2 = findViewById(R.id.play2)
        button.setOnClickListener(this)
        button2.setOnClickListener(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        var buffer:ByteBuffer = ByteBuffer.allocateDirect(1024);
//        var buffer1:ByteBuffer = ByteBuffer.allocate(1024);
//        buffer.put(buffer1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            PICK_VIDEO_RESULT_CODE -> {
                val uri=data?.data
                Log.i("gq","" + uri?.path)
                var input = uri?.let { contentResolver.openFileDescriptor(it, "r")?.fileDescriptor }
                Thread(Runnable {
                    render29(input, surfaceView.holder.surface)
                }).start()
            }
        }
    }

    fun getRealPathFromURI(contentUri: Uri): String {
        var res: String = ""
        val proj = arrayOf<String>(MediaStore.Video.Media.DATA)
        val cursor: Cursor = contentResolver.query(contentUri, proj, null, null, null)!!
        if (cursor.moveToFirst()) {
            val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    override fun onClick(v: View?) {
        // TODO("Not yet implemented")
        when(v?.id) {
            R.id.play->{
                if (Build.VERSION.SDK_INT >= 29) {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "video/*"
                    startActivityForResult(intent, PICK_VIDEO_RESULT_CODE)
                    return
                }
                val inputFile = File(
                    Environment.getExternalStorageDirectory(),
                    "tencent/MicroMsg/WeiXin/wx_camera_1580477719299.mp4"
                )
                if (!inputFile.exists()) {
                    return
                }
                var input = inputFile.absolutePath
                Thread(Runnable {
                    render(input, surfaceView.holder.surface)
                }).start()
            }
            R.id.play2 -> {
                startActivity(Intent(this@MainActivity, GLSurfaceViewActivity::class.java))
            }

        }

    }


}
