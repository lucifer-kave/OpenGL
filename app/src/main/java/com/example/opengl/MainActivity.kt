package com.example.opengl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.opengl.Jni.render
import com.example.opengl.Jni.render29
import com.example.opengl.databinding.ActivityMainBinding
import com.miracle.commonlib.mroute.MRoute
import com.miracle.commonlib.mroute.core.RootUriHandler
import com.miracle.commonlib.mroute.core.UriRequest
import com.miracle.router.annotation.Router
//import io.flutter.embedding.android.FlutterActivity
//import io.flutter.embedding.android.FlutterView
import java.io.File
import java.io.FileDescriptor

@Router(path = ["/main"])
class MainActivity : FragmentActivity(), View.OnClickListener {
    private val PICK_VIDEO_RESULT_CODE = 101;
    private lateinit var surfaceView: SurfaceView
    private lateinit var glSurfaceView: VideoGLSurfaceView
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        surfaceView = SurfaceView(this)
        mBinding.surfaceContainer.addView(surfaceView)
        mBinding.play.setOnClickListener(this)
        mBinding.play2.setOnClickListener(this)
        mBinding.play3.setOnClickListener(this)
        mBinding.play4.setOnClickListener(this)
        mBinding.record.setOnClickListener(this)


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        MRoute.init(RootUriHandler())

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        var buffer:ByteBuffer = ByteBuffer.allocateDirect(1024);
//        var buffer1:ByteBuffer = ByteBuffer.allocate(1024);
//        buffer.put(buffer1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_VIDEO_RESULT_CODE -> {
                val uri: Uri = data?.data!!
                Log.i("gq", "" + uri.path)
                val input: FileDescriptor = contentResolver.openFileDescriptor(uri, "r")!!.fileDescriptor
                Handler().postDelayed(Runnable {
                    render29(input, surfaceView.holder.surface)
                },1000)
            }
        }
    }

    override fun onClick(v: View?) {
        // TODO("Not yet implemented")
        when (v?.id) {
            R.id.play -> {
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
//                    Jni.startAudioPlayer()
                }).start()
            }
            R.id.play2 -> {
                startActivity(Intent(this@MainActivity, GLSurfaceViewActivity::class.java))
            }
            R.id.play3 -> {
//                startActivity(FlutterActivity.createDefaultIntent(this@MainActivity))
            }
            R.id.play4 -> {
                startActivity(Intent(this@MainActivity, FlutterFragmentActivity::class.java))
            }
            R.id.record -> {
                startActivity(Intent(this@MainActivity, RECActivity::class.java))
            }
        }

    }


}
