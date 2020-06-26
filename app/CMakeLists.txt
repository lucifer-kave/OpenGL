cmake_minimum_required(VERSION 3.4.1)
include_directories(libs/include)
set(DIR ../../../../libs)
add_library(avcodec-58
        SHARED
        IMPORTED)
set_target_properties(avcodec-58
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libavcodec-58.so)

add_library(avdevice-58
        SHARED
        IMPORTED)
set_target_properties(avdevice-58
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libavdevice-58.so)
add_library(avformat-58
        SHARED
        IMPORTED)
set_target_properties(avformat-58
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libavformat-58.so)
add_library(avutil-56
        SHARED
        IMPORTED)
set_target_properties(avutil-56
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libavutil-56.so)
add_library(postproc-55
        SHARED
        IMPORTED)
set_target_properties(postproc-55
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libpostproc-55.so)
add_library(swresample-3
        SHARED
        IMPORTED)
set_target_properties(swresample-3
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libswresample-3.so)
add_library(swscale-5
        SHARED
        IMPORTED)
set_target_properties(swscale-5
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libswscale-5.so)
add_library(avfilter-7
        SHARED
        IMPORTED)
set_target_properties(avfilter-7
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/armeabi-v7a/libavfilter-7.so)

add_library(native-lib
        SHARED
        src/main/jni/Test.cpp
        src/main/jni/shader.cpp
        src/main/jni/player.cpp
        src/main/jni/audio.cpp
        src/main/jni/audio-jni.cpp
        src/main/jni/util.cpp)

find_library(log-lib
        log)

target_link_libraries(native-lib
        android
        EGL
        GLESv2
        OpenSLES
        avfilter-7
        avcodec-58
        avdevice-58
        avformat-58
        avutil-56
        postproc-55
        swresample-3
        swscale-5
        ${log-lib})