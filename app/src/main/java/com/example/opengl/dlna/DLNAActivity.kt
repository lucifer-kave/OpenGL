package com.example.opengl.dlna

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset
import com.example.opengl.R

class DLNAActivity : AppCompatActivity() {
    val requestData = "M-SEARCH * HTTP/1.1 \n" +
            "MX: 1 \n" + //最大时间间隔数
            "ST: upnp:rootdevice \n" +//搜索的设备类型
            "MAN: \"ssdp:discover\" \n" +
            "User-Agent: iOS 10.2.1 product/version\n" +
            "Connection: close\n" +
            "Host: 239.255.255.250 "//多播地址
    var datagramSocket = DatagramSocket()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dlnaactivity)
    }

    val datagramSocketSend = DatagramSocket()
    private fun funUDPSend(targetIp: String, targetPort: Int, msg: String) {
        val toByteArray = msg.toByteArray(Charset.forName("UTF-8"))
        val outPacket = DatagramPacket(ByteArray(0), 0, InetAddress.getByName(targetIp), targetPort)
        try {
            //注意，这里每次新建一个UDP，并且发送完毕后关闭，也可以新建一个全局UDP
            //退出时再关闭，不要和接收用同一个UDP，否则可能会信息错乱
            outPacket.data = toByteArray
            datagramSocketSend.send(outPacket)
            Log.e("UDP Send Message:", msg)
            //  datagramSocketSend.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UDP Send Error!", e.toString())
        }
    }

    //UDP接收
    //需要添加子线程
    private fun funUDPReceive(port: Int) {
        val byteArray = ByteArray(4096)
        val inPacket = DatagramPacket(byteArray, byteArray.size)
        try {
            datagramSocket = DatagramSocket(port)
            Log.e("UDP", "成功开启UDP接收")
            while (true) {
                datagramSocket.receive(inPacket)
                val string = String(byteArray, 0, inPacket.length, Charset.forName("UTF-8"))
                Log.e("UDPReceive", string)
//                val message = Message()
//                val bundle = Bundle()
//                bundle.putString(
//                    UDP_RECEIVE_HANDLER_BUNDLE,
//                    inPacket.socketAddress.toString().substring(1) + ":" + string
//                )
//                message.what = UDP_RECEIVE_HANDLER_MESSAGE
//                message.data = bundle
//                handle.sendMessage(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("error", e.toString())
//            val message = Message()
//            val bundle = Bundle()
//            bundle.putString(HANDLER_UDP_RECEIVE_FAILED_BUNDLE,e.toString())
//            message.what = HANDLER_UDP_RECEIVE_FAILED_MSG
//            message.data = bundle
//            handle.sendMessage(message)
            datagramSocket.close()
        }
    }
}