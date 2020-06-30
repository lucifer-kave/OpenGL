package com.example.opengl

import android.view.Surface
import java.io.FileDescriptor

object Jni {
    init {
        System.loadLibrary("avcodec-58")
        System.loadLibrary("avdevice-58")
        System.loadLibrary("avfilter-7")
        System.loadLibrary("avformat-58")
        System.loadLibrary("avutil-56")
        System.loadLibrary("postproc-55")
        System.loadLibrary("swresample-3")
        System.loadLibrary("swscale-5")
        System.loadLibrary("native-lib")
    }

    external fun render29(input: FileDescriptor, surface: Surface)

    external fun render(input: String, surface: Surface)

    external fun resumePlayer():Int

    external fun pausePlayer():Int

    external fun stopPlayer():Int
}