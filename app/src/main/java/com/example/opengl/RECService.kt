package com.example.opengl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.IOException


class RECService: Service() {
    private var mediaRecorder: MediaRecorder? = null
    private val mBinder:RECBinder = RECBinder()
    var mediaProjection: MediaProjection? = null
    private var running = false
    private var width = 720
    private var height = 1080
    private var dpi = 360

    inner class RECBinder : Binder() {
        val service:RECService
            get() = this@RECService
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        buildNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onDestroy() {
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
        initRecorder()
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
        mediaRecorder?.setOutputFile("videoPath");
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
        private const val CHANNEL_ID = "com.example.opengl.RECService"
        private const val CHANNEL_NAME = "com.example.opengl"
    }
}