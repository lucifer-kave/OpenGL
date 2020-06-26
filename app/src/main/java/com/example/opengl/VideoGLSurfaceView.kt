package com.example.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class VideoGLSurfaceView : GLSurfaceView {
    private val TAG = "VideoGLSurfaceView"
    private val TOUCH_SCALE_FACTOR = 180.0f / 320
    private var mPreviousX = 0f
    private var mPreviousY = 0f

    constructor(context:Context) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        var videoRender = VideoRender()
        setRenderer(videoRender)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    class VideoRender:Renderer, SurfaceTexture.OnFrameAvailableListener {
        private val TAG = "VideoRender"
        private var mTextureID = 0
        private var mMediaPlayer: MediaPlayer? = null
        private val mSTMatrix = FloatArray(16)
        private lateinit var mSurface: SurfaceTexture
        private var updateSurface = false
        override fun onDrawFrame(gl: GL10?) {
            // TODO("Not yet implemented")
            synchronized(this) {
                if (updateSurface) {
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(mSTMatrix);
                    updateSurface = false;
                }
            }

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            // TODO("Not yet implemented")
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // TODO("Not yet implemented")

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)

            mTextureID = textures[0]
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
//            checkGlError("glBindTexture mTextureID")

            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat()
            )
            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )

            mSurface = SurfaceTexture(mTextureID);
            mSurface.setOnFrameAvailableListener(this);
            val surface = Surface(mSurface)
            mMediaPlayer?.setSurface(surface)
            mMediaPlayer?.setScreenOnWhilePlaying(true)
            surface.release()

            try {
                mMediaPlayer?.prepare()
            } catch (t: IOException) {
                Log.e(TAG, "media player prepare failed")
            }

            synchronized(this) { updateSurface = false }

            mMediaPlayer?.start()
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
            // TODO("Not yet implemented")
            updateSurface = true
        }

    }
}