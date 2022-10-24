package com.example.opengl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.opengl.webscket.WebSocketService
import okio.ByteString.Companion.toByteString
import java.io.*
import java.util.*


class RECService: Service() {
    private var mediaRecorder: MediaRecorder? = null
    private val mBinder:RECBinder = RECBinder()
    var mediaProjection: MediaProjection? = null
    private var running = false
    private var width = 720
    private var height = 1080
    private var dpi = 360
    private var vEncoder:MediaCodec? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var isVideoEncoder = false
    private var vBufferInfo:MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    var mWebSocketService :WebSocketService? = null

    inner class RECBinder : Binder() {
        val service:RECService
            get() = this@RECService
    }

    override fun onCreate() {
        Log.e(TAG,"onCreate")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG,"onStartCommand")
        buildNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e(TAG,"onBind")
        buildNotification()
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopRecord()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopRecord()
        super.onDestroy()
    }

    fun buildNotification() {
        registerNotificationChannel()
        val notifyId = System.currentTimeMillis().toInt()
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        mBuilder //必须要有
            .setSmallIcon(R.mipmap.ic_launcher) //可选

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mBuilder.setContentTitle(resources.getString(R.string.app_name))
        }
        startForeground(notifyId, mBuilder.build())
    }

    /**
     * 注册通知通道
     */
    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mNotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = mNotificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                )
                //是否在桌面icon右上角展示小红点
                channel.enableLights(true)
                //小红点颜色
                channel.lightColor = Color.RED
                //通知显示
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                //是否在久按桌面图标时显示此渠道的通知
                //channel.setShowBadge(true);
                mNotificationManager.createNotificationChannel(channel)
            }
        }
    }

    //服务的两个主要逻辑
    //开始录屏
    fun startRecord() : Boolean{
        //首先判断是否有录屏工具以及是否在录屏
        if (mediaProjection == null || running) {
            return false;
        }
        //有录屏工具，没有在录屏，就进行录屏
        //初始化录像机，录音机Recorder
//        initRecorder()
        prepareVideoEncoder(width, height);
        //根据获取的屏幕参数创建虚拟的录屏屏幕
        createVirtualDisplay()
        //本来不加异常也可以，但是这样就不知道是否start成功
        //万一start没有成功，但是running置为true了，就产生了错误也无提示
        //提示开始录屏了，但是并没有工作
        try{
            //准备工作都完成了，可以开始录屏了
            mediaRecorder?.start()
            //标志位改为正在录屏
            running = true
            return true;
        }catch (e:Exception){
            e.printStackTrace();
            //有异常，start出错，没有开始录屏，弹出提示
            Toast.makeText(this,"开启失败，没有开始录屏",Toast.LENGTH_SHORT).show();
            //标志位变回没有录屏的状态
            running = false;
            return false;
        }
    }

    //初始化Recorder录像机
    private fun initRecorder() {
        //新建Recorder
        mediaRecorder = MediaRecorder();
        //设置录像机的一系列参数
        //设置音频来源
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频来源
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置视频格式为mp4
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //保存在该位置
        mediaRecorder?.setOutputFile("mH264DataFile");
        //设置视频大小，清晰度
        mediaRecorder?.setVideoSize(width, height);
        //设置视频编码为H.264
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置音频编码
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //设置视频码率
        mediaRecorder?.setVideoEncodingBitRate(2 * 1920 * 1080);
        mediaRecorder?.setVideoFrameRate(18);
        //初始化完成，进入准备阶段，准备被使用
        //截获异常，处理
        try {
            mediaRecorder?.prepare();
        } catch (e: IOException) {
            e.printStackTrace();
            //异常提示
            Toast.makeText(this,
                "Recorder录像机prepare失败，无法使用，请重新初始化！",
                Toast.LENGTH_SHORT).show();
        }
    }

    @Throws(IOException::class)
    fun prepareVideoEncoder(width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
        format.setInteger(MediaFormat.KEY_WIDTH, width)
        format.setInteger(MediaFormat.KEY_HEIGHT, height)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        // 当画面静止时,重复最后一帧，不影响界面显示(好像并没有什么用)
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, (1000000 / 20).toLong())
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface: Surface = encoder.createInputSurface()
        mVirtualDisplay = mediaProjection?.createVirtualDisplay(
            "-display", width, height, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null
        )
        vEncoder = encoder
        startVideo()
    }

    fun startVideo() {
        Thread{
            try {
                vEncoder?.start();


                while (isVideoEncoder && !Thread.interrupted()) {
                    try {

                        var outputBufferId = vEncoder?.dequeueOutputBuffer(vBufferInfo, 0);

                        if (outputBufferId != null && outputBufferId >= 0) {

                            // 有效输出
                            // 获取到的实时帧视频数据
                            var encodedData = vEncoder?.getOutputBuffer(outputBufferId)

                            val dataToWrite = ByteArray(vBufferInfo.size)
                            encodedData?.get(dataToWrite, 0, vBufferInfo.size);
                            mWebSocketService?.sendData(dataToWrite.toByteString())
//
                            vEncoder?.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (e:Exception) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (e:Exception) {
                isVideoEncoder = false
                if (null != vEncoder) {
                    vEncoder?.stop()
                }
                if (mVirtualDisplay != null) mVirtualDisplay?.release()
                if (mediaProjection != null) mediaProjection?.stop()
            }
        }.start()
        isVideoEncoder = true
    }

    fun stopRecord() {
        isVideoEncoder = false
        vEncoder?.stop()
        mVirtualDisplay?.release()
        mediaProjection?.stop()
    }

    fun createVirtualDisplay() {
        //虚拟屏幕通过MediaProjection获取，传入一系列传过来的参数
        //可能创建时会出错，捕获异常
        try {
            var virtualDisplay =
                mediaProjection?.createVirtualDisplay(
                    "VirtualScreen",
                    width,
                    height,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder?.getSurface(),
                    null,
                    null
                );
        }catch (e:Exception){
            e.printStackTrace();
            Toast.makeText(this,"virtualDisplay创建录屏异常，请退出重试！",Toast.LENGTH_SHORT).show();
        }
    }

    companion object {
        private const val TAG = "RECService"
        private const val CHANNEL_ID = "com.example.opengl.RECService"
        private const val CHANNEL_NAME = "com.example.opengl"
    }
}