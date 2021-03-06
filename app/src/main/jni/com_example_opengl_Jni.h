//
// Created by guoqi5 on 2020/6/23.
//
#include <jni.h>
#ifndef OPENGL_COM_EXAMPLE_OPENGL_JNI_H
#define OPENGL_COM_EXAMPLE_OPENGL_JNI_H
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_player_ffmpeg_VideoSurface
 * Method:    nativePausePlayer
 * Signature: ()I
 */
JNIEXPORT void JNICALL Java_com_example_opengl_Jni_render29(JNIEnv *env, jobject obj, jobject input, jobject surface);

JNIEXPORT void JNICALL Java_com_example_opengl_Jni_render(JNIEnv *env, jobject obj, jstring input, jobject surface);

/*
 * Class:     com_opensles_ffmpeg_MainActivity
 * Method:    stopPlayer
 * Signature: ()I
 */JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_stopPlayer(JNIEnv *, jobject);

/*
* Class:     com_opensles_ffmpeg_MainActivity
* Method:    destroyEngine
* Signature: ()I
*/JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_resumePlayer(
        JNIEnv *, jobject);

/*
* Class:     com_opensles_ffmpeg_MainActivity
* Method:    startAudioPlayer
* Signature: ()I
*/JNIEXPORT jint JNICALL Java_com_example_opengl_Jni_pausePlayer(
        JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif //OPENGL_COM_EXAMPLE_OPENGL_JNI_H
