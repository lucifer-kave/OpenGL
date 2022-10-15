//
// Created by guoqi23 on 2022/8/17.
//
#include "ffmpeg_api.h"

#include <jni.h>
#include <string.h>
#include <android/bitmap.h>
#include "stdlib.h"
#include "mobilenetssdncnn.h"
#include "convert_frame.h"

#ifdef __cplusplus
extern "C" {
#endif
#include <libswscale/swscale.h>
#include <libavutil/frame.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/dict.h>
#ifdef __cplusplus
}
#endif
#include "android_log.h"
#define GQ_DEBUG

static jclass m_clazz = NULL;//当前类(面向java)

/**
 * 回调执行Java方法
 * 参看 Jni反射+Java反射
 */
void apiCallJavaMethod(JNIEnv *env, jclass clazz, jobjectArray ret, int64_t time) {
    if (clazz == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return;
    }
    //获取方法ID (I)V指的是方法签名 通过javap -s -public FFmpegCmd 命令生成
    jmethodID methodID = env->GetStaticMethodID(clazz, "onExecuted",
                                                "([Lcom/meituan/navi/ncnn/MobilenetSSDNcnn$Obj;I)V");
    if (methodID == NULL) {
        LOGE("---------------methodID isNULL---------------");
        return;
    }
    //调用该java方法
    env->CallStaticVoidMethod(clazz, methodID, ret, time);
}

void apiDebugCallJavaMethod(JNIEnv *env, jclass clazz, jobject bitmap, int64_t time) {
    if (clazz == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return;
    }
    //获取方法ID (I)V指的是方法签名 通过javap -s -public FFmpegCmd 命令生成
    jmethodID debugMethodID = env->GetStaticMethodID(clazz, "onExecutedBitmap",
                                                     "(Landroid/graphics/Bitmap;I)V");
    if (debugMethodID == NULL) {
        LOGE("---------------debugMethodID isNULL---------------");
        return;
    }
    //调用该java方法
    env->CallStaticVoidMethod(clazz, debugMethodID, bitmap, time);
}

void yuv420ToRgb(AVFrame *frame, uint8_t **rgb) {
    int img_width = frame->width;
    int img_height = frame->height;
    int channels = 4;

    uint8_t *buffer = *rgb;

    for (int y = 0; y < img_height; y++) {
        for (int x = 0; x < img_width; x++) {

            //linesize[0]表示一行Y数据需要多少字节存储, 由于字节对齐的优化,一般会大于图片的宽度,例如,测试视频linesize[0]为864,img_width为854
            int indexY = y * frame->linesize[0] + x;
            int indexU = y / 2 * frame->linesize[1] + x / 2;
            int indexV = y / 2 * frame->linesize[2] + x / 2;
            uint8_t Y = frame->data[0][indexY];
            uint8_t U = frame->data[1][indexU];
            uint8_t V = frame->data[2][indexV];

            // 这里可以参考YUV420转rgb公式
            int R = Y + 1.402 * (V - 128);  // 由于计算的结果可能不在0~255之间,所以R不能用uint8_t表示
            int G = Y - 0.34413 * (U - 128) - 0.71414 * (V - 128);
            int B = Y + 1.772 * (U - 128);
            R = (R < 0) ? 0 : R;
            G = (G < 0) ? 0 : G;
            B = (B < 0) ? 0 : B;
            R = (R > 255) ? 255 : R;
            G = (G > 255) ? 255 : G;
            B = (B > 255) ? 255 : B;
            buffer[(y * img_width + x) * channels + 0] = (uint8_t) R;
            buffer[(y * img_width + x) * channels + 1] = (uint8_t) G;
            buffer[(y * img_width + x) * channels + 2] = (uint8_t) B;
            //补充 alpha通道数据, android转bitmap需要
            buffer[(y * img_width + x) * channels + 3] = 0xff;
        }
    }
}

jobject createBitmap(JNIEnv *env,
                     int width, int height) {

    jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapFunction = env->GetStaticMethodID(bitmapCls,
                                                            "createBitmap",
                                                            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID valueOfBitmapConfigFunction = env->GetStaticMethodID(bitmapConfigClass,
                                                                   "valueOf",
                                                                   "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");

    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass,
                                                       valueOfBitmapConfigFunction,
                                                       configName);

    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls,
                                                    createBitmapFunction,
                                                    width, height,
                                                    bitmapConfig);

    return newBitmap;
}

jobjectArray createObjectArray(JNIEnv *env,struct Object* objects,int obj_num){
    // objects to Obj[]
    const char* class_names[] = {"stop","air_sign","warn","passage"};

    jclass objCls = env->FindClass("com/meituan/navi/ncnn/MobilenetSSDNcnn$Obj");
    jobjectArray jObjArray = env->NewObjectArray(obj_num, objCls, NULL);

    jfieldID xId = env->GetFieldID(objCls, "x", "F");
    jfieldID yId = env->GetFieldID(objCls, "y", "F");
    jfieldID wId = env->GetFieldID(objCls, "w", "F");
    jfieldID hId = env->GetFieldID(objCls, "h", "F");
    jfieldID labelId = env->GetFieldID(objCls, "label", "Ljava/lang/String;");
    jfieldID probId = env->GetFieldID(objCls, "prob", "F");

    for (size_t i=0; i<obj_num; i++){
        jobject jObj = env->AllocObject(objCls);

        env->SetFloatField(jObj, xId, objects[i].x);
        env->SetFloatField(jObj, yId, objects[i].y);
        env->SetFloatField(jObj, wId, objects[i].w);
        env->SetFloatField(jObj, hId, objects[i].h);
        env->SetObjectField(jObj, labelId, env->NewStringUTF(class_names[objects[i].label]));
        env->SetFloatField(jObj, probId, objects[i].prob);

        env->SetObjectArrayElement(jObjArray, i, jObj);
    }

    return jObjArray;
}


void writeToBitmap(JNIEnv *env, jobject bmp, AVFrame *frame) {
    int ret;
    if (!bmp) {
        LOGI("bmp == null");
    }
    void *addr_pixels;
    ret = AndroidBitmap_lockPixels(env, bmp, &addr_pixels);
    if (ret < 0) {
        LOGI("lockPixel error");
        return;
    }
    AndroidBitmapInfo info;
    ret = AndroidBitmap_getInfo(env, bmp, &info);
    if (ret < 0) {
        LOGI("getInfo error");
        return;
    }
    yuv420ToRgb(frame, reinterpret_cast<uint8_t **>(&addr_pixels));
    AndroidBitmap_unlockPixels(env, bmp);
}

void writeToBitmap2(JNIEnv *env, jobject bmp, const unsigned char* rgb,int w,int h){
    int ret;
    if (!bmp) {
        LOGI("bmp == null");
    }
    void *addr_pixels;
    ret = AndroidBitmap_lockPixels(env, bmp, &addr_pixels);
    if (ret < 0) {
        LOGI("lockPixel error");
        return;
    }
    AndroidBitmapInfo info;
    ret = AndroidBitmap_getInfo(env, bmp, &info);
    if (ret < 0) {
        LOGI("getInfo error");
        return;
    }

    int channels=4;
    uint8_t* dst_add= static_cast<uint8_t *>(addr_pixels);
    for(int y=0;y<h;++y){
        for(int x=0;x<w;++x){
            dst_add[(y * w + x) * channels + 0] = (uint8_t) rgb[(y * w + x) * channels + 0];
            dst_add[(y * w + x) * channels + 1] = (uint8_t) rgb[(y * w + x) * channels + 1];
            dst_add[(y * w + x) * channels + 2] = (uint8_t) rgb[(y * w + x) * channels + 2];
            dst_add[(y * w + x) * channels + 3] = 0xff;
        }
    }

    AndroidBitmap_unlockPixels(env, bmp);
}

void writeToBitmap3(JNIEnv *env, jobject bmp, AVFrame *frame) {
    int ret;
    if (!bmp) {
        LOGI("bmp == null");
    }
    void *addr_pixels;
    ret = AndroidBitmap_lockPixels(env, bmp, &addr_pixels);
    if (ret < 0) {
        LOGI("lockPixel error");
        return;
    }
    AndroidBitmapInfo info;
    ret = AndroidBitmap_getInfo(env, bmp, &info);
    if (ret < 0) {
        LOGI("getInfo error");
        return;
    }
//    AVFrame *rgb_frame = av_frame_alloc();
//    struct SwsContext* swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,avCodecContext->pix_fmt,
//                                                   avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,SWS_BICUBIC,NULL,NULL,NULL);
//    sws_scale(swsContext, (const uint8_t* const*)frame->data, frame->linesize, 0, avCodecContext->height, rgb_frame->data, rgb_frame->linesize);
    //yuv420ToRgb(frame,&addr_pixels);
//    fill_bitmap(&info, addr_pixels, rgb_frame);

    int img_w = frame->width;
    int img_h = frame->height;
    int channels=4;
    unsigned char* img_addr= static_cast<unsigned char *>(addr_pixels);
    int img_stride=frame->linesize[0];
    for(int i=0;i<img_h;++i){
        for(int j=0;j<img_w;++j){
            img_addr[(i * img_w + j) * channels + 0] = (uint8_t) frame->data[0][i*img_stride+j*3+0];
            img_addr[(i * img_w + j) * channels + 1] = (uint8_t) frame->data[0][i*img_stride+j*3+1];
            img_addr[(i * img_w + j) * channels + 2] = (uint8_t) frame->data[0][i*img_stride+j*3+2];
            //补充 alpha通道数据, android转bitmap需要
            img_addr[(i * img_w + j) * channels + 3] = 0xff;
        }
    }

    AndroidBitmap_unlockPixels(env, bmp);
}

void copyFromFrame(AVFrame *frame,unsigned char* dst){
    int img_w = frame->width;
    int img_h = frame->height;
    int img_stride=frame->linesize[0];
    unsigned char* src=frame->data[0];
    for(int i=0;i<img_h;++i){
        memcpy(dst,src,img_w*3*sizeof(unsigned char));
        dst+=img_w*3;
        src+=img_stride;
    }
}

void open_file_cut_image(JNIEnv *env, char *inputFile, int64_t *startTimes, int length) {
    AVFormatContext *avFormatContext = NULL;
    const AVCodec *avCodec;
    AVCodecContext *avCodecContext;
    AVFrame *frame;
    char buf[] = "";
    int video_index = -1;
    int error = avformat_open_input(&avFormatContext, inputFile, NULL, NULL);
    if (error < 0) {
        av_strerror(error, buf, 2048);
        LOGE("Couldn't open file : %d(%s)", error, buf);
        LOGE("打开视频失败");
        return;
    }

    if (avformat_find_stream_info(avFormatContext, NULL) < 0) {
        LOGE("获取内容失败");
        return;
    }

    int ret = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot find a video stream in the input file\n");
        return;
    }
    video_index = ret;
    LOGE("成功找到视频流");
    AVCodecParameters *codecpar = avFormatContext->streams[video_index]->codecpar;
    avCodec = avcodec_find_decoder(codecpar->codec_id);
    avCodecContext = avcodec_alloc_context3(avCodec);
    avcodec_parameters_to_context(avCodecContext, codecpar);

    if (avcodec_open2(avCodecContext, avCodec, NULL) < 0) {
        LOGE("打开失败");
        return;
    }

    // 读取一帧数据
    AVPacket *packet = av_packet_alloc();
    bool useTest = false;
    if (length == 0) {
        int64_t during = avFormatContext->streams[video_index]->duration *
                         av_q2d(avFormatContext->streams[video_index]->time_base);
        LOGE("视频时长:%lld", during);
        length = during;
        useTest = true;
    }

    int total_count=0,count=0;
    for (int i = 0; i < length; i++) {
        int64_t startTime = useTest ? i * 1000 : startTimes[i];
        // seek到固定的位置
        float pos = startTime / 1000.0;
        int64_t pts = pos / av_q2d(avFormatContext->streams[video_index]->time_base);


        // 改变播放进度
        // 单位是秒,是double类型
        ret = av_seek_frame(avFormatContext, video_index, pts, AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
        if (ret < 0) {
            break;
        }
        while (true) {
            ret = av_read_frame(avFormatContext, packet);
            if (ret < 0) {
                break;
            }
            if (packet->stream_index == video_index) {
                ret = avcodec_send_packet(avCodecContext, packet);
                av_packet_unref(packet);
                if (ret < 0) {
                    av_log(NULL, AV_LOG_ERROR, "Error while sending a packet to the decoder\n");
                    break;
                }
                frame = av_frame_alloc();
                ret = avcodec_receive_frame(avCodecContext, frame);
                if (ret == AVERROR_EOF) {
                    break;
                } else if (ret == AVERROR(EAGAIN)) {
                    continue;
                } else if (ret < 0) {
                    av_log(NULL, AV_LOG_ERROR, "Error while receiving a frame from the decoder\n");
                    break;
                }
                if (frame->pts >= pts && frame->data[0] != NULL) {
                    total_count++;
                    int dst_w = 0, dst_h = 0;
                    int ori_w = frame->width, ori_h = frame->height;
                    float scale = 1.0;
                    get_img_size(ori_w,ori_h,&dst_w,&dst_h,&scale);
                    scale = 1.0;

                    struct SwsContext *sws_resize;
                    sws_resize = sws_getContext(ori_w, ori_h, AV_PIX_FMT_YUV420P, dst_w, dst_h, AV_PIX_FMT_RGB24, SWS_BILINEAR, NULL, NULL, NULL);
                    AVFrame* resize_frame = av_frame_alloc();
                    int sws_ret=sws_scale_frame(sws_resize,resize_frame,frame);
                    sws_freeContext(sws_resize);

                    av_frame_unref(frame);
                    av_frame_free(&frame);

                    struct Object* objects=NULL;
                    int obj_num=0;
                    detect_img(resize_frame->data[0],resize_frame->width,resize_frame->height,resize_frame->linesize[0],ori_w,ori_h,scale,&objects,&obj_num);
#ifdef GQ_DEBUG
                    jobject bmp = createBitmap(env, resize_frame->width, resize_frame->height);
                    writeToBitmap3(env, bmp, resize_frame);
                    apiDebugCallJavaMethod(env, m_clazz, bmp, startTime);
#endif
                    av_frame_unref(resize_frame);
                    av_frame_free(&resize_frame);
                    if(obj_num>0) count++;

                    jobjectArray jobject_arr= createObjectArray(env,objects,obj_num);
                    free(objects);
                    objects=NULL;
                    __android_log_print(ANDROID_LOG_ERROR, "FFMPEG", "j object array: %d",env->GetArrayLength(jobject_arr));

                    apiCallJavaMethod(env, m_clazz, jobject_arr, startTime);

                    break;
                }
                av_frame_unref(frame);
                av_frame_free(&frame);
            }
        }
    }
    __android_log_print(ANDROID_LOG_ERROR, "FFMPEG", "count:%d,total count:%d",count,total_count);

    av_packet_free(&packet);
    avcodec_close(avCodecContext);
    avformat_close_input(&avFormatContext);
}

extern "C" {
JNIEXPORT void JNICALL
Java_com_meituan_navi_ncnn_FFmpegJNI_cutImage(JNIEnv *env, jclass clazz, jstring input,
                                                     jlongArray startTime) {
    m_clazz = clazz;
    const char *inputPath = env->GetStringUTFChars(input, JNI_FALSE);
    int32_t length = env->GetArrayLength(startTime);
    int64_t *array = env->GetLongArrayElements(startTime, JNI_FALSE);

    open_file_cut_image(env, const_cast<char *>(inputPath), array, length);
    env->ReleaseStringUTFChars(input, inputPath);
    env->ReleaseLongArrayElements(startTime, array, 0);
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_meituan_navi_ncnn_FFmpegJNI_aesEncode(JNIEnv *env, jclass clazz, jstring content) {
    jstring key = env->NewStringUTF("YCTdcdMO5TQ4qgx2Gu3Tlfc4l2yWaQVF");
    jclass aesCls = env->FindClass("com/meituan/navi/ncnn/utils/AesUtil");
    if (aesCls == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return NULL;
    }
    jmethodID methodID = env->GetStaticMethodID(aesCls, "encode",
                                                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (methodID == NULL) {
        LOGE("---------------methodID isNULL---------------");
        return NULL;
    }
    return (jstring) env->CallStaticObjectMethod(aesCls, methodID, key, content);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_meituan_navi_ncnn_FFmpegJNI_aesDecode(JNIEnv *env, jclass clazz, jstring content) {
    jstring key = env->NewStringUTF("YCTdcdMO5TQ4qgx2Gu3Tlfc4l2yWaQVF");
    jclass aesCls = env->FindClass("com/meituan/navi/ncnn/utils/AesUtil");
    if (aesCls == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return NULL;
    }
    jmethodID methodID = env->GetStaticMethodID(aesCls, "decode",
                                                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (methodID == NULL) {
        LOGE("---------------methodID isNULL---------------");
        return NULL;
    }
    return (jstring) env->CallStaticObjectMethod(aesCls, methodID, key, content);
}