package com.example.opengl.webscket

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import okhttp3.*
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString
import java.net.URI
import java.util.concurrent.TimeUnit


class WebSocketService: Service() {
    private var mBinder:WebSocketBinder = WebSocketBinder()
    private var okHttpClient: OkHttpClient? = null
    private var client:RealWebSocket? = null

    inner class WebSocketBinder : Binder() {
        val service: WebSocketService
            get() = this@WebSocketService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initSocketClient()
        return super.onStartCommand(intent, flags, startId)
    }

    //这里是处理webscoket
    private fun initSocketClient() {
        val url = "ws://端口号:ip地址" //协议标识符是ws
        okHttpClient = OkHttpClient.Builder()
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)//设置连接超时时间
            .retryOnConnectionFailure(true)//断线重连
            .build()
        val request: Request = Request.Builder().url(url).build()
        client = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.d(TAG, "onClose() 连接断开_reason：" + reason )
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                Log.d(TAG, "onClosing() 连接断开_reason：" + reason )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.d(TAG, "onFailure() 连接出错：" + t.message )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                //message就是接收到的消息
                Log.d(TAG, "WebSocketService收到的消息：" + text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.d(TAG, "WebSocket 连接成功" )
            }
        }) as RealWebSocket
    }

    /**
     * 断开连接
     */
    fun closeConnect() {
        try {
            client?.cancel()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            client = null
        }
    }

    fun sendData(byteString: ByteString) {
        client?.send(byteString)
    }

    companion object {
        private const val DEFAULT_HEART_TIME: Long = 60//默认心跳时间
        private const val READ_TIMEOUT: Long = 30//读取超时
        private const val TAG: String = "WebSocketLog"//日志抬头
        private const val WRITE_TIMEOUT: Long = 30//写入超时
        private const val CONNECT_TIMEOUT: Long = 30//连接超时
        private const val WHAT_HEART: Int = 0//handler-what
    }
}