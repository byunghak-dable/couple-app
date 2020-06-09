package org.personal.coupleapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Message
import android.util.Log
import org.personal.coupleapp.backgroundOperation.SocketReceiverThread
import org.personal.coupleapp.backgroundOperation.SocketSenderThread
import org.personal.coupleapp.backgroundOperation.SocketSenderThread.Companion.SEND_MESSAGE
import org.personal.coupleapp.interfaces.service.ChatListener
import org.personal.coupleapp.utils.serverConnection.TCPClient
import java.lang.ref.WeakReference

class ChatService : Service(), SocketReceiverThread.ChatRespondListener {

    private val TAG = javaClass.name

    private val binder: IBinder = LocalBinder()
    private lateinit var socketSenderThread: SocketSenderThread
    private lateinit var socketReceiverThread: SocketReceiverThread

    private var chatListenerWeak : WeakReference<ChatListener>? = null

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
        sendMessage(SEND_MESSAGE, "quit")
        // 스레드가 바로 종료 되지 않도록 0.5초 후에 스레드 종료
        socketReceiverThread.isStop = true
        socketSenderThread.looper.quit()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ChatService? {

            return this@ChatService
        }
    }

    fun setOnChatRespondListener(listener: ChatListener) {
        chatListenerWeak = WeakReference(listener)
    }

    //------------------ 액티비티에서 사용할 메소드 모음 ------------------

    fun sendChatMessage(messageData: String) {
        sendMessage(SEND_MESSAGE, messageData)
    }

    override fun onReceive(respond: String?) {
        // 서버 통신이 끊겼을 때 null 값을 읽어드린다(예외 처리)
        if (!respond.isNullOrEmpty()) {
            chatListenerWeak?.get()!!.onReceiveChat(respond)
        }
    }

    // 서버에 메시지를 보내는 메소드(socketSenderThread 에게 메시지를 보낸다)
    private fun sendMessage(whichMessage: Int, chatMessage: String) {
        val message = Message.obtain(socketSenderThread.getHandler())
        message.what = whichMessage
        message.obj = chatMessage
        message.sendToTarget()
        Log.i(TAG, "메시지 발신 테스트 : ")
    }
}