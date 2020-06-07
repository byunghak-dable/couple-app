package org.personal.coupleapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import org.personal.coupleapp.backgroundOperation.SocketReceiverThread
import org.personal.coupleapp.backgroundOperation.SocketSenderThread
import org.personal.coupleapp.backgroundOperation.SocketSenderThread.Companion.SEND_MESSAGE
import org.personal.coupleapp.utils.serverConnection.TCPClient
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class ChatService : Service(), SocketReceiverThread.ChatRespondListener {

    private val TAG = javaClass.name

    private val binder: IBinder = LocalBinder()
    private lateinit var socketSenderThread: SocketSenderThread
    private lateinit var socketReceiverThread: SocketReceiverThread

    private var chatRespondListener: ChatRespondListener? = null

    override fun onCreate() {
        super.onCreate()
        /*
         메인 스레드에서 socket 생성할 수 없기 때문에 스레드 생성해서 객체 생성
         1. SocketSenderThread : 메시지를 보내는 스레드
         2. SocketReceiverThread : 서버에서 보내는 메시지를 읽는 메소드
         */
        Thread(Runnable {
            val tcpClient = TCPClient("13.125.99.215", 20205)
            if (tcpClient.connect()) {
                Log.i(TAG, "서비스 연결 완료")
                socketSenderThread = SocketSenderThread("socketSenderThread", tcpClient)
                socketReceiverThread = SocketReceiverThread(tcpClient, this)

                socketSenderThread.start()
                socketReceiverThread.start()
            } else {
                Log.i(TAG, "서비스 연결 실패")
            }
        }).start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // 스레드 종료
    override fun onDestroy() {
        super.onDestroy()
        // 클라이언트 소켓 제거 하도록 메시지 전송
        HandlerMessageHelper.sendChatMessage(socketSenderThread, SEND_MESSAGE, "quit")
        // 스레드가 바로 종료 되지 않도록 0.5초 후에 스레드 종료
        Handler().postDelayed({
            socketReceiverThread.isStop = true
            socketSenderThread.looper.quit()
        }, 500)
    }

    inner class LocalBinder : Binder() {
        fun getService(): ChatService? {

            return this@ChatService
        }
    }

    // 액티비티에서 채팅 메시지를 받기 위한 인터페이스
    interface ChatRespondListener {
        fun onReceiveChat(respond: String?)
    }

    //
    fun setOnChatRespondListener(listener: ChatRespondListener) {
        chatRespondListener = listener
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------

    fun sendMessage(message: String) {
        HandlerMessageHelper.sendChatMessage(socketSenderThread, SEND_MESSAGE, message)
    }

    override fun onReceive(respond: String?) {
        chatRespondListener!!.onReceiveChat(respond)
    }
}