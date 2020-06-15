package org.personal.coupleapp.backgroundOperation

import android.util.Log
import org.personal.coupleapp.utils.serverConnection.TCPClient

class SocketReceiverThread(private val tcpClient: TCPClient, private val chatRespondListener: ChatRespondListener) : Thread() {

    private val TAG = javaClass.name

    var isStop = false

    override fun run() {
        super.run()

        while (!isStop) {

            val respond = tcpClient.readMessage()
            chatRespondListener.onReceive(respond)
        }
        tcpClient.socketClose()
        Log.i(TAG , "ReceiverThread 종료")
    }

    interface ChatRespondListener{
        fun onReceive(respond:String?)
    }
}