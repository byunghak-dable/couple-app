package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.utils.serverConnection.HTTPRequest

class ServerConnectionThread(name: String?, private val mainHandler: Handler) : HandlerThread(name) {

    // msg.what, msg.arg1 의 value 값 선언
    companion object {
        const val FETCH_DATA = 1
        const val POST_DATA = 2
        const val REQUEST_SIMPLE_POSTING = 1
        const val REQUEST_PROFILE_UPLOAD = 2

    }

    private val TAG = javaClass.name

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        Log.i("thread-test", "onLooperPrepared")

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                // mainHandler 의 msg.what 값은 msg.arg1 에서 추출
                val whichMessage = msg.arg1

                when (msg.what) {

                    // 서버로부터 데이터를 받아오고 결과를 UIHandler 로 전송
                    FETCH_DATA -> {
                        val serverPage = msg.obj.toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)

                        message.obj = httpRequest.fetchFromServer()
                        message.what = whichMessage
                        message.sendToTarget()
                    }

                    // 서버에 데이터를 전송하고 서버에서 받은 결과를 UIHandler 로 전송
                    POST_DATA -> {
                        // 어떤 HTTPRequest 메소드를 사용할 지 정함
                        val whichRequest = msg.arg2
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)
                        val postData: Any

                        when (whichRequest) {
                            // 간단한 데이터(메인 스레드에서 jsonString 으로 만들어서 보내는 경우)
                            REQUEST_SIMPLE_POSTING -> {
                                postData = msgObjHashMap["postData"].toString()
                                message.obj = httpRequest.postToServer(postData)
                            }

                            // 프로필 데이터 보내는 경우
                            REQUEST_PROFILE_UPLOAD -> {
                                postData = msgObjHashMap["postData"] as ProfileData
                                message.obj = httpRequest.postProfileToServer(postData)
                            }
                        }

                        message.what = whichMessage
                        Log.i(TAG, message.obj.toString())
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