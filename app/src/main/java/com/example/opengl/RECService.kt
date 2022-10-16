package com.example.opengl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat


class RECService: Service() {
    val CHANNEL_ID = "com.example.opengl.RECService"
    val CHANNEL_NAME = "com.example.opengl"

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        buildNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
}