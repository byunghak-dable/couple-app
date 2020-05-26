package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import org.personal.coupleapp.utils.serverConnection.HTTPRequest

class ServerConnectionThread(name: String?, private val mainHandler: Handler) : HandlerThread(name) {

    private val TAG = javaClass.name

    // msg.what 의 value 값 선언
    companion object {
        const val FETCH_DATA = 1
        const val POST_DATA = 2
    }

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {

                    // 서버로부터 데이터를 받아오고 결과를 UIHandler 로 전송
                    FETCH_DATA -> {
                        val serverPage = msg.obj.toString()
                        // mainHandler 의 msg.what 값은 msg.arg1 에서 추출
                        val whichMessage = msg.arg1
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)

                        message.obj = httpRequest.fetchFromServer()
                        message.what = whichMessage
                        message.sendToTarget()
                    }

                    // 서버에 데이터를 전송하고 서버에서 받은 결과를 UIHandler 로 전송
                    POST_DATA -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        // mainHandler 의 msg.what 값은 msg.arg1 에서 추출
                        val whichMessage = msg.arg1
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val postJasonString = msgObjHashMap["postJsonString"].toString()

                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)

                        message.obj = httpRequest.postToServer(postJasonString)
                        message.what = whichMessage
                        message.sendToTarget()
                    }
                }
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}