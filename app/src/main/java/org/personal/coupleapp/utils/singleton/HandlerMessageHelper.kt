package org.personal.coupleapp.utils.singleton

import android.os.Message
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_DELETE
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.GET_REQUEST
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.POST_REQUEST
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.PUT_REQUEST

object HandlerMessageHelper {

    private val TAG = javaClass.name

    // --------------- 액티비티에서 서버와 통신하기 위해 ServerConnectionThread 로 메시지를 보내는 메소드 모음 ---------------
    // GET 메소드로 요청을 보낼 때
    fun serverGetRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, whichMessage: Int, whichRequest:Int) {
        val message = Message.obtain(serverConnectionThread.getHandler())

        message.obj = serverPage
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.arg2 = whichRequest
        message.what = GET_REQUEST
        message.sendToTarget()
    }

    // POST 메소드로 요청을 보낼 때
    fun serverPostRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, postData: Any, whichMessage: Int, whichRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(serverConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["postData"] = postData

        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.arg2 = whichRequest
        message.what = POST_REQUEST
        message.sendToTarget()
    }

    // POST 메소드로 요청을 보낼 때
    fun serverPutRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, putData: Any, whichMessage: Int, whichRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(serverConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["putData"] = putData

        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.arg2 = whichRequest
        message.what = PUT_REQUEST
        message.sendToTarget()
    }

    // POST 메소드로 요청을 보낼 때
    fun serverDeleteRequest(serverConnectionThread: ServerConnectionThread, serverPage: String, deleteData: Any, whichMessage: Int, whichRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(serverConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["deleteData"] = deleteData

        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichMessage
        message.arg2 = whichRequest
        message.what = REQUEST_DELETE
        message.sendToTarget()
    }

    // decodingThread 에게 이미지 uri 를 보내는 메소드
    fun decodeImage(imageDecodeThread: ImageDecodeThread, targetImageData: Any?, whichMessage: Int, mainMessage: Int) {
        val message: Message = Message.obtain(imageDecodeThread.getHandler())
        message.obj = targetImageData
        message.what = whichMessage
        message.arg1 = mainMessage
        message.sendToTarget()
    }
}