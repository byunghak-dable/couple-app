package org.personal.coupleapp.utils.singleton

import android.os.Message
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.FETCH_DATA
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.POST_DATA

object HandlerMessageHelper {

    // --------------- 액티비티에서 서버와 통신하기 위해 ServerConnectionThread 로 메시지를 보내는 메소드 모음 ---------------
    // GET 메소드로 요청을 보낼 때
    fun serverGetRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, whichMessage: Int) {
        val message = Message.obtain(serverConnectionThread.getHandler())

        message.obj = serverPage
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.what = FETCH_DATA
        message.sendToTarget()
    }

    // POST 메소드로 요청을 보낼 때
    fun serverPostRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, postJsonString: String, whichMessage: Int, whichRequest:Int) {
        val messageObject = HashMap<String, String>()
        val message = Message.obtain(serverConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["postJsonString"] = postJsonString

        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.arg2 = whichRequest
        message.what = POST_DATA
        message.sendToTarget()
    }
}