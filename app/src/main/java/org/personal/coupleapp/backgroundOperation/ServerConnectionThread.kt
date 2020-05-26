package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import org.personal.coupleapp.utils.serverConnection.HTTPRequest

class ServerConnectionThread(name: String?, priority: Int) : HandlerThread(name, priority) {

    private val TAG = javaClass.name

    // msg.what 의 value 값 선언
    private val FETCH_DATA = 1
    private val POST_DATA = 2
    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {

                    FETCH_DATA -> fetchDataFromServer(msg.obj.toString())
                    POST_DATA -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val postJasonString = msgObjHashMap["postJasonString"].toString()
                        postDataToServer(serverPage, postJasonString)
                    }
               }
            }
            // TODO: 서버로 부터 받아온 데이터 handle 해야함

            // 서버로부터 데이터를 받아오는 메소드
            private fun fetchDataFromServer(serverPage: String) {
                val httpRequest = HTTPRequest(serverPage)
                httpRequest.fetchFromServer()
            }

            // 서버에 데이터를 전송하는 메소드
            private fun postDataToServer(serverPage: String, postJasonString: String) {
                val httpRequest = HTTPRequest(serverPage)
                httpRequest.postToServer(postJasonString)
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}