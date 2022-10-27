package com.example.opengl

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import com.example.opengl.R


class GLSurfaceViewActivity : FragmentActivity() {
    private lateinit var glSurfaceView: MyGLSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glsurfaceview)
        glSurfaceView = MyGLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2)
        findViewById<RelativeLayout>(R.id.surface_container).addView(glSurfaceView)
        findViewById<RelativeLayout>(R.id.surface_container).setOnTouchListener { v: View?, event: MotionEvent? ->

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