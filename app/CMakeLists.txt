cmake_minimum_required(VERSION 3.4.1)
include_directories(src/main/jni/include)
set(DIR ../../../../libs)
add_library(avcodec
        STATIC
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/${CMAKE_ANDROID_ARCH_ABI}/libavcodec.a)

add_library(avformat
        STATIC
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/${CMAKE_ANDROID_ARCH_ABI}/libavformat.a)

add_library(avutil
        STATIC
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/${CMAKE_ANDROID_ARCH_ABI}/libavutil.a)

add_library(swresample
        STATIC
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/${CMAKE_ANDROID_ARCH_ABI}/libswresample.a)

add_library(swscale
        STATIC
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${DIR}/${CMAKE_ANDROID_ARCH_ABI}/libswscale.a)

add_library(native-lib
        SHARED
        src/main/jni/Test.cpp
#        src/main/jni/shader.cpp
#        src/main/jni/player.cpp
#        src/main/jni/audio.cpp
#        src/main/jni/util.cpp
#        src/main/jni/video.cpp
        )

find_library(log-lib
        log)

target_link_libraries(native-lib
        android
        EGL
        GLESv2
        OpenSLES
        avformat
        avcodec
        avutil
        swresample
        swscale
        z
        ${log-lib})