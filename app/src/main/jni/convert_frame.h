#ifndef NCNN_ANDROID_MOBILENETSSD_CONVERT_FRAME_H
#define NCNN_ANDROID_MOBILENETSSD_CONVERT_FRAME_H

#ifdef __cplusplus
extern "C" {
#endif

void resize_bilinear_yuv420p(const unsigned char* srcy, int srcw, int srch, int srcy_stride,
                             const unsigned char* srcu,int srcu_stride,
                             const unsigned char* srcv,int srcv_stride,
                             unsigned char* dst, int dstw, int dsth);

void yuv420p2rgb(const unsigned char* yuv420p, int w, int h, unsigned char* rgb);

#ifdef __cplusplus
}
#endif

#endif