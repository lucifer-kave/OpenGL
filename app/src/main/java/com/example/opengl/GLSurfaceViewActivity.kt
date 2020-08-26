package com.example.opengl

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import com.example.opengl.databinding.ActivityGlsurfaceviewBinding

class GLSurfaceViewActivity : AppCompatActivity() {
    private lateinit var glSurfaceView: MyGLSurfaceView
    private lateinit var mBinding: ActivityGlsurfaceviewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityGlsurfaceviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        glSurfaceView = MyGLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2)
        mBinding.surfaceContainer.addView(glSurfaceView)
        mBinding.surfaceContainer.setOnTouchListener { v: View?, event: MotionEvent? ->

            return@setOnTouchListener false
        }
        glSurfaceView.initRender()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}