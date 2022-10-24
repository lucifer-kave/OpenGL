//
// Created by guoqi5 on 2020/5/12.
//
#include <jni.h>
#include <android/native_window_jni.h>
#include "player.h"
#include "com_example_opengl_Jni.h"
GlobalContext global_context;

#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"LC",FORMAT,##__VA_ARGS__);

static ANativeWindow* mANativeWindow;
static ANativeWindow_Buffer nwBuffer;

void SaveFrame(AVFrame *pFrame, int width, int height, int iFrame) {
    FILE *pFile;
    char szFilename[32];
    int y;

    // Open file.
    sprintf(szFilename, "frame%d.ppm", iFrame);
    pFile = fopen(szFilename, "wb");
    if (pFile == NULL) {
        return;
    }

    // Write header.
    fprintf(pFile, "P6\n%d %d\n255\n", width, height);

    // Write pixel data.
    for (y = 0; y < height; y++) {
        fwrite(pFrame->data[0]+y*pFrame->linesize[0], 1, width*3, pFile);
    }

    // Close file.
    fclose(pFile);
}

void renderSurface(uint8_t *pixel) {

//    if (global_context.pause) {
//        return;
//    }
//                ANativeWindow_lock(mANativeWindow, &native_outBuffer, NULL);
//                uint8_t *dst = (uint8_t *)native_outBuffer.bits;
//                int destStride = native_outBuffer.stride * 4;
//                uint8_t * src = rgb_frame->data[0];
//                int srcStride = rgb_frame->linesize[0];
//
//                for (int i = 0; i < avCodecContext->height; ++i) {
//                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
//                }
//                ANativeWindow_unlockAndPost(mANativeWindow);
//    ANativeWindow_acquire(mANativeWindow);
//
//    if (0 != ANativeWindow_lock(mANativeWindow, &nwBuffer, NULL)) {
//        LOGE("ANativeWindow_lock() error");
//        return;
//    }
//    //LOGV("renderSurface, %d, %d, %d", nwBuffer.width ,nwBuffer.height, nwBuffer.stride);
//    if (nwBuffer.width >= nwBuffer.stride) {
//        //srand(time(NULL));
//        //memset(piexels, rand() % 100, nwBuffer.width * nwBuffer.height * 2);
//        //memcpy(nwBuffer.bits, piexels, nwBuffer.width * nwBuffer.height * 2);
//        memcpy(nwBuffer.bits, pixel, nwBuffer.width * nwBuffer.height * 2);
//    } else {
//        LOGE("new buffer width is %d,height is %d ,stride is %d",
//             nwBuffer.width, nwBuffer.height, nwBuffer.stride);
//        int i;
//        for (i = 0; i < nwBuffer.height; ++i) {
//            memcpy((void*) ((int) nwBuffer.bits + nwBuffer.stride * i * 4),
//                   (void*) ((int) pixel + nwBuffer.width * i * 4),
//                   nwBuffer.width * 4);
//        }
//    }
//
//    if (0 != ANativeWindow_unlockAndPost(mANativeWindow)) {
//        LOGE("ANativeWindow_unlockAndPost error");
//        return;
//    }
//
//    ANativeWindow_release(mANativeWindow);
    if (!IS_USE_YUV) {
//        Render(pixel);
    }
}

void renderSurface(AVFrame *avFrame) {
    LOGI("renderSurface")
//    Render(avFrame);
}


int eglOpen() {
    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY );
    if (eglDisplay == EGL_NO_DISPLAY ) {
        LOGV("eglGetDisplay failure.");
        return -1;
    }
    global_context.eglDisplay = eglDisplay;
    LOGV("eglGetDisplay ok");

    EGLint majorVersion;
    EGLint minorVersion;
    EGLBoolean success = eglInitialize(eglDisplay, &majorVersion,
                                       &minorVersion);
    if (!success) {
        LOGV("eglInitialize failure.");
        return -1;
    }
    LOGV("eglInitialize ok");

    GLint numConfigs;
    EGLConfig config;
    static const EGLint CONFIG_ATTRIBS[] = { EGL_BUFFER_SIZE, EGL_DONT_CARE,
                                             EGL_RED_SIZE, 5, EGL_GREEN_SIZE, 6, EGL_BLUE_SIZE, 5,
                                             EGL_DEPTH_SIZE, 16, EGL_ALPHA_SIZE, EGL_DONT_CARE, EGL_STENCIL_SIZE,
                                             EGL_DONT_CARE, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                                             EGL_SURFACE_TYPE, EGL_WINDOW_BIT, EGL_NONE // the end
    };
    success = eglChooseConfig(eglDisplay, CONFIG_ATTRIBS, &config, 1,
                              &numConfigs);
    if (!success) {
        LOGV("eglChooseConfig failure.");
        return -1;
    }
    LOGV("eglChooseConfig ok");

    const EGLint attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    EGLContext elgContext = eglCreateContext(eglDisplay, config, EGL_NO_CONTEXT,
                                             attribs);
    if (elgContext == EGL_NO_CONTEXT ) {
        LOGV("eglCreateContext failure, error is %d", eglGetError());
        return -1;
    }
    global_context.eglContext = elgContext;
    LOGV("eglCreateContext ok");

    EGLint eglFormat;
    success = eglGetConfigAttrib(eglDisplay, config, EGL_NATIVE_VISUAL_ID,
                                 &eglFormat);
    if (!success) {
        LOGV("eglGetConfigAttrib failure.");
        return -1;
    }
    global_context.eglFormat = eglFormat;
    LOGV("eglGetConfigAttrib ok");

    EGLSurface eglSurface = eglCreateWindowSurface(eglDisplay, config,
                                                   mANativeWindow, 0);
    if (NULL == eglSurface) {
        LOGV("eglCreateWindowSurface failure.");
        return -1;
    }
    global_context.eglSurface = eglSurface;
    LOGV("eglCreateWindowSurface ok");
    return 0;
}

int eglClose() {
    EGLBoolean success = eglDestroySurface(global_context.eglDisplay,
                                           global_context.eglSurface);
    if (!success) {
        LOGV("eglDestroySurface failure.");
    }

    success = eglDestroyContext(global_context.eglDisplay,
                                global_context.eglContext);
    if (!success) {
        LOGV("eglDestroySurface failure.");
    }

    success = eglTerminate(global_context.eglDisplay);
    if (!success) {
        LOGV("eglDestroySurface failure.");
    }

    global_context.eglSurface = NULL;
    global_context.eglContext = NULL;
    global_context.eglDisplay = NULL;

    return 0;
}

int32_t setBuffersGeometry(int32_t width, int32_t height) {
    //int32_t format = WINDOW_FORMAT_RGB_565;

    if (NULL == mANativeWindow) {
        LOGV("mANativeWindow is NULL.");
        return -1;
    }

    return ANativeWindow_setBuffersGeometry(mANativeWindow, width, height,
                                            global_context.eglFormat);
}

//读取数据的回调函数-------------------------
//AVIOContext使用的回调函数！
//注意：返回值是读取的字节数
//手动初始化AVIOContext只需要两个东西：内容来源的buffer，和读取这个Buffer到FFmpeg中的函数
//int fill_iobuffer(void * buffer,uint8_t *iobuf, int bufsize){
//    if(!feof(fp_open)){
//        int true_size=fread(buf,1,buf_size,fp_open);
//        return true_size;
//    }else{
//        return -1;
//    }
//}

static int jniGetFDFromFileDescriptor(JNIEnv * env, jobject fileDescriptor) {
    jint fd = -1;
    jclass fdClass = env->FindClass("java/io/FileDescriptor");

    if (fdClass != NULL) {
        jfieldID fdClassDescriptorFieldID = env->GetFieldID(fdClass, "descriptor", "I");
        if (fdClassDescriptorFieldID != NULL && fileDescriptor != NULL) {
            fd = env->GetIntField(fileDescriptor, fdClassDescriptorFieldID);
        }
    }

    return fd;
}

void player_video() {
    AVFormatContext *avFormatContext = NULL;
    AVPacket *packet;
    AVCodecContext *avCodecContext;
    const AVCodec *avCodec;
    pthread_t thread;
    int error;
    char buf[] = "";
    avformat_network_init();
    int video_index = -1;
    if ((error = avformat_open_input(&avFormatContext, global_context.inputPath, NULL, NULL)) < 0) {
        av_strerror(error, buf, 2048);
        LOGE("Couldn't open file : %d(%s)", error, buf);
        LOGE("打开视频失败")
        return;
    }

    if (avformat_find_stream_info(avFormatContext, NULL) < 0) {
        LOGE("获取内容失败");
        return;
    }

    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }
    LOGE("成功找到视频流")
    AVCodecParameters *avCodecParameters = avFormatContext->streams[video_index]->codecpar;
    avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    avCodecContext = avcodec_alloc_context3(avCodec);
    if (avcodec_open2(avCodecContext, avCodec, NULL) < 0) {
        LOGE("打开失败")
        return;
    }

    global_context.vcodec_ctx = avCodecContext;
    global_context.vstream = avFormatContext->streams[video_index];
    global_context.vcodec = avCodec;

    ANativeWindow_setBuffersGeometry(mANativeWindow, avCodecContext->width, avCodecContext->height, global_context.eglFormat);

    packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    av_init_packet(packet);
    AVFrame *frame = av_frame_alloc();
    if (!IS_USE_YUV) {
        AVFrame *rgb_frame = av_frame_alloc();
        uint8_t *out_buffer = (uint8_t *)av_malloc(av_image_get_buffer_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height, 1));
        av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, out_buffer, AV_PIX_FMT_YUV420P,
                             avCodecContext->width, avCodecContext->height, 1);
    }

    /**
     * RGB565是这样初始化的
      AVPicture picture;
      av_image_alloc(picture.data, picture.linesize,
                     avCodecContext->width,
                     avCodecContext->height, AV_PIX_FMT_RGB565LE, 16);**/
    if (!IS_USE_YUV) {
        struct SwsContext* swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,avCodecContext->pix_fmt,
                avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,SWS_BICUBIC,NULL,NULL,NULL);
    }
//    pthread_create(&thread, NULL, video_thread_1, NULL);

//    int frameCount;
//    int h = 0;
    LOGE("解码");
    while (av_read_frame(avFormatContext, packet) >= 0) {
        LOGE("解码 %d",packet->stream_index)
        LOGE("VINDEX %d",video_index)
        if (packet->stream_index == video_index) {
//            packet_queue_put(&global_context.video_queue, packet);
//            LOGE("解码 hhhhh");
//            avcodec_decode_video2(global_context.vcodec_ctx, frame, &frameCount, packet);
//            avcodec_decode_video2(avCodecContext, frame, &frameCount, packet);
//            LOGE("解码中....  %d",frameCount)
//            if (frameCount) {
//                LOGE("转换并绘制")
//                if (!IS_USE_YUV) {
//                    sws_scale(swsContext, (const uint8_t *const *)frame->data, frame->linesize, 0, frame->height, rgb_frame->data, rgb_frame->linesize);
//                    renderSurface(rgb_frame->data[0]);
//                } else{
//                    renderSurface(frame);
//                }
//                SaveFrame(rgb_frame, avCodecContext->width,avCodecContext->height,1);
//                ANativeWindow_lock(mANativeWindow, &native_outBuffer, NULL);
//                uint8_t *dst = (uint8_t *)native_outBuffer.bits;
//                int destStride = native_outBuffer.stride * 4;
//                uint8_t * src = rgb_frame->data[0];
//                int srcStride = rgb_frame->linesize[0];
//
//                for (int i = 0; i < avCodecContext->height; ++i) {
//                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
//                }
//                ANativeWindow_unlockAndPost(mANativeWindow);
//                usleep(1000 * 16);

//            }

        } else {
            av_packet_unref(packet);
        }

    }

    while (!global_context.quit) {
        usleep(1000);
    }

//    ANativeWindow_release(mANativeWindow);
    av_frame_free(&frame);
//    av_frame_free(&rgb_frame);
    avcodec_close(avCodecContext);
    if (avFormatContext) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
    }

    avformat_network_deinit();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_opengl_Jni_render29(JNIEnv *env, jobject obj, jobject inputfd, jobject surface) {
    int fd = jniGetFDFromFileDescriptor(env, inputfd);

//    avFormatContext = avformat_alloc_context();
//    unsigned char * iobuffer=(unsigned char *)av_malloc(32768);
//    AVIOContext *avio = avio_alloc_context(iobuffer, 32768, 0, NULL, fill_iobuffer, NULL, NULL);
//    avFormatContext->pb = avio;
    char path[20];
    sprintf(path, "pipe:%d", fd);
    global_context.inputPath = path;
//    FILE *file = fdopen(fd, "rb");

//    if (file && (fseek(file, offset, SEEK_SET) == 0)) {
//        char str[20];
//        sprintf(str, "pipe:%d", fd);
//        strcat(path, str);
//    }
    global_context.inputPath = path;
    mANativeWindow = ANativeWindow_fromSurface(env,surface);
    if (mANativeWindow == 0) {
        LOGE("nativewindow取到失败 error = %d", mANativeWindow)
        return;
    }

    if ((global_context.eglSurface != NULL)
        || (global_context.eglContext != NULL)
        || (global_context.eglDisplay != NULL)) {
        eglClose();
    }
    eglOpen();

    player_video();

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_opengl_Jni_render(JNIEnv *env, jobject obj, jstring input, jobject surface) {
    const char* inputPath = env->GetStringUTFChars(input, JNI_FALSE);
    global_context.inputPath = inputPath;
    pthread_t thread;
    mANativeWindow = ANativeWindow_fromSurface(env,surface);
    if (mANativeWindow == 0) {
        LOGE("nativewindow取到失败 error = %d", mANativeWindow)
        return;
    }
    if ((global_context.eglSurface != NULL)
        || (global_context.eglContext != NULL)
        || (global_context.eglDisplay != NULL)) {
        eglClose();
    }
    eglOpen();
    player_video();
//    pthread_create(&thread, NULL, open_media, NULL);
    env->ReleaseStringUTFChars(input, inputPath);
}

/*
 * Class:     com_example_opengl_Jni
 * Method:    resumePlayer
 * Signature: ()I
 */ extern "C" JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_resumePlayer(
        JNIEnv *, jobject) {
    global_context.pause = 0;
    return 0;
}

/*
 * Class:     com_example_opengl_Jni
 * Method:    pausePlayer
 * Signature: ()I
 */ extern "C" JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_pausePlayer(
        JNIEnv *, jobject) {
    global_context.pause = 1;
    return 0;
}

/*
 * Class:     com_example_opengl_Jni
 * Method:    stopPlayer
 * Signature: ()I
 */ extern "C" JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_stopPlayer(
        JNIEnv *, jobject) {
//    destroyPlayerAndEngine();
    eglClose();
    global_context.pause = 1;
    global_context.quit = 1;
    usleep(50000);
    return 0;
}




