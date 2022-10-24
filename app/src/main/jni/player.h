//
// Created by guoqi5 on 2020/6/12.
//

#ifndef OPENGL_PLAYER_H
#define OPENGL_PLAYER_H

#ifdef __cplusplus
extern "C" {
#endif
#include <unistd.h>
#include <stdbool.h>

#include <assert.h>
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <android/log.h>
#include <time.h>
#include <utime.h>

#include <errno.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/syscall.h>
#include <sched.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "libavcodec/avcodec.h"
#include "libavutil/log.h"
#include "libavutil/time.h"
#include "libavutil/samplefmt.h"
#include "libavutil/opt.h"
#include "libavutil/channel_layout.h"
#include "libavutil/imgutils.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
//#include "libavfilter/avfilter.h"
//#include "libavfilter/buffersink.h"
//#include "libavfilter/buffersrc.h"
#include <libavutil/imgutils.h>

#define AVCODEC_MAX_AUDIO_FRAME_SIZE 192000 // 1 second of 48khz 32bit audio
#define IS_USE_YUV 1
#define VIDEO_PICTURE_QUEUE_SIZE 30

enum {
    AV_SYNC_AUDIO_MASTER, AV_SYNC_VIDEO_MASTER, AV_SYNC_EXTERNAL_MASTER,
};

typedef struct PacketQueue {
    AVPacketList *first_pkt, *last_pkt;
    int nb_packets;
    int size;
    int abort_request;
    int serial;
    pthread_mutex_t mutex;
} PacketQueue;

typedef struct AudioParams {
    int freq;
    int channels;
    unsigned int channel_layout;
    enum AVSampleFormat fmt;
    int frame_size;
    int bytes_per_sec;
} AudioParams;

typedef struct VideoPicture {
    AVFrame *pFrame;
    int width, height;
    double pts;
} VideoPicture;

typedef struct GlobalContexts {
    EGLDisplay eglDisplay;
    EGLSurface eglSurface;
    EGLContext eglContext;
    EGLint eglFormat;

    GLuint mTextureID[3];
    GLuint glProgram;
    GLint positionLoc;

    AVCodecContext *acodec_ctx;
    AVCodecContext *vcodec_ctx;
    AVStream *vstream;
    AVStream *astream;
    const AVCodec *vcodec;
    const AVCodec *acodec;

    PacketQueue video_queue;
    PacketQueue audio_queue;

    pthread_mutex_t pictq_mutex;
    pthread_cond_t pictq_cond;

    pthread_mutex_t timer_mutex;
    pthread_cond_t timer_cond;

    double frame_timer;
    double frame_last_delay;
    double frame_last_pts;

    int64_t video_current_pts_time;

    int pictq_windex;
    int pictq_rindex;
    int pictq_size;

    VideoPicture pictq[VIDEO_PICTURE_QUEUE_SIZE];

    const char* inputPath;

    int quit;
    int pause;
} GlobalContext;

double get_master_clock();
double get_audio_clock();
double get_video_clock();

void packet_queue_init(PacketQueue *q);
int packet_queue_get(PacketQueue *q, AVPacket *pkt);
int packet_queue_put(PacketQueue *q, AVPacket *pkt);

int32_t setBuffersGeometry(int32_t width, int32_t height);
int audio_decode_frame(uint8_t *audio_buf, int buf_size);
void* open_media(void *argv);
void *video_thread(void *argv);
void *video_thread_1(void *argv);
void* picture_thread(void *argv);
int createEngine();
int createBufferQueueAudioPlayer();
void fireOnPlayer();

void SaveFrame(AVFrame *pFrame, int width, int height, int iFrame);
//void renderSurface(uint8_t *pixel);
void renderSurface(AVFrame *frame);
//void Render(uint8_t *pixel);
void Render(AVFrame *frame);
int CreateProgram();
int eglClose();
void destroyPlayerAndEngine();

extern GlobalContext global_context;

#define TAG "FFmpeg"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,TAG,FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,TAG,FORMAT,##__VA_ARGS__);
#define LOGV2(FORMAT, ...) __android_log_print(ANDROID_LOG_VERBOSE,TAG,FORMAT,##__VA_ARGS__);

#ifdef __cplusplus
}
#endif

#endif //OPENGL_PLAYER_H
