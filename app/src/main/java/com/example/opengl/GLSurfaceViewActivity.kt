package com.example.opengl

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout

class GLSurfaceViewActivity : AppCompatActivity() {
    private lateinit var surface: RelativeLayout
    private lateinit var glSurfaceView: MyGLSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glsurfaceview)
        surface = findViewById(R.id.surface_container);
        glSurfaceView = MyGLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2)
        surface.setOnTouchListener { v: View?, event: MotionEvent? ->

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