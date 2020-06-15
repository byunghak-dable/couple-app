package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.annotation.RequiresApi
import org.personal.coupleapp.utils.serverConnection.TCPClient

class SocketSenderThread(name: String?, val tcpClient: TCPClient) : HandlerThread(name) {

    companion object {
        const val SEND_MESSAGE = 1
    }

    private val TAG = javaClass.name

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler() {

            @RequiresApi(Build.VERSION_CODES.P)
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when (msg.what) {

                    SEND_MESSAGE -> {
                        tcpClient.writeMessage(msg.obj.toString())
                        Log.i(TAG, "전송?")

                    }
                }
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}