package com.example.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import com.miracle.router.annotation.Router
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


@Router(path = ["/my"])
class MyGLSurfaceView : GLSurfaceView {
    private val TAG = "VideoGLSurfaceView"
    private val TOUCH_SCALE_FACTOR = 180.0f / 320
    private var mPreviousX = 0f
    private var mPreviousY = 0f
    private var mRenderer: TriangleRender? = null

    constructor(context:Context) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    public fun initRender() {
        mRenderer = TriangleRender()
        setRenderer(mRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun init() {
    }

    class TriangleRender : Renderer {

        private var mMVPMatrixHandle: Int =0
        private var mColorHandle: Int = 0
        private val COORDS_PER_VERTEX: Int = 3

        private var mPositionHandle: Int = 0
        val triangleCoords = floatArrayOf(0.5f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f)
        val color = floatArrayOf(1.0f, 0f, 0f, 1.0f)
        private lateinit var vertexBuffer: FloatBuffer
        private var mProgram = 0;
        // 添加uMVPMatrix与vPosition相乘
        private var vertexShaderCode: String =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}"
        private var fragmentShaderCode: String =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}"
        private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
        private val vertexStride: Int = COORDS_PER_VERTEX * 4
        private val mProjectionMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)
        private val mMVPMatrix = FloatArray(16)
        private val mRotationMatrix = FloatArray(16)

        @Volatile
        private var mAngle = 0f

        override fun onDrawFrame(gl: GL10?) {
            // TODO("Not yet implemented")
            val scratch = FloatArray(16)
            Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0f, 0f, -1.0f)
            Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0)

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(mProgram)
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(
                mPositionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
            GLES20.glUniform4fv(mColorHandle, 1, color, 0)

            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, scratch, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(mPositionHandle)

        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            // TODO("Not yet implemented")
            GLES20.glViewport(0, 0, width, height)

            val ratio = width.toFloat() / height
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // TODO("Not yet implemented")
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)

            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            var bb: ByteBuffer = ByteBuffer.allocateDirect(triangleCoords.size * 4)
            bb.order(ByteOrder.nativeOrder())

            vertexBuffer = bb.asFloatBuffer()
            vertexBuffer.put(triangleCoords)
            vertexBuffer.position(0)

            var vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            var fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(mProgram, vertexShader)
            GLES20.glAttachShader(mProgram, fragmentShader)
            GLES20.glLinkProgram(mProgram)
        }

        fun loadShader(type: Int, shaderCode: String): Int {
            var shader: Int = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }

        /**
         * Returns the rotation angle of the triangle shape (mTriangle).
         *
         * @return - A float representing the rotation angle.
         */
        fun getAngle(): Float {
            return mAngle
        }

        /**
         * Sets the rotation angle of the triangle shape (mTriangle).
         */
        fun setAngle(angle: Float) {
            mAngle = angle
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x: Float = event?.getX() ?: 0f
        val y: Float = event?.getY() ?: 0f

        when (event?.getAction()) {
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = x - mPreviousX
                val dy: Float = y - mPreviousY
                mRenderer?.getAngle()?.plus((dx + dy) * TOUCH_SCALE_FACTOR)?.let {
                    mRenderer?.setAngle(
                        it
                    )
                } // = 180.0f / 320
                requestRender()
            }
        }

        mPreviousX = x
        mPreviousY = y
        return true
    }
}