package org.personal.coupleapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Message
import android.util.Log
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.GET_REQUEST
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.POST_REQUEST
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.PUT_REQUEST
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.DELETE_REQUEST
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import java.lang.ref.WeakReference

class HTTPConnectionService : Service(), HTTPConnectionListener {

    private val TAG = javaClass.name

    private val binder: IBinder = LocalBinder()
    private lateinit var httpConnectionThread: HTTPConnectionThread
    private var httpListenerWeak : WeakReference<HTTPConnectionListener>? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "서비스 생명 주기 : OnCreate")
        httpConnectionThread = HTTPConnectionThread("HTTPConnectionThread", this)
        httpConnectionThread.start()
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
        Log.i(TAG, "서비스 생명 주기 :  Destroyed")
        httpConnectionThread.looper.quit()
    }

    inner class LocalBinder : Binder() {
        fun getService(): HTTPConnectionService? {

            return this@HTTPConnectionService
        }
    }

    fun setOnHttpRespondListener(listener: HTTPConnectionListener) {
        httpListenerWeak = WeakReference(listener)
    }

    // 서버 통신 결과를 보내주는 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        // 서비스에서 받은 결과를 다시 인터페이스를 이용해 메인으로 보냄
        httpListenerWeak?.get()!!.onHttpRespond(responseData)
    }


    // --------------- 액티비티에서 서버와 통신하기 위해 ServerConnectionThread 로 메시지를 보내는 메소드 모음 ---------------
    // GET 메소드로 요청을 보낼 때
    fun serverGetRequest(serverPage: String, whichThreadRequest: Int, whichMainRequest: Int) {
        val message = Message.obtain(httpConnectionThread.getHandler())

        message.what = GET_REQUEST
        message.obj = serverPage
        message.arg1 = whichThreadRequest
        message.arg2 = whichMainRequest
        message.sendToTarget()
    }

    // POST 메소드로 요청을 보낼 때
    fun serverPostRequest(serverPage: String, postData: Any, whichThreadRequest: Int, whichMainRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(httpConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["postData"] = postData

        message.what = POST_REQUEST
        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichThreadRequest
        message.arg2 = whichMainRequest
        message.sendToTarget()
    }

    // PUT 메소드로 요청을 보낼 때
    fun serverPutRequest(serverPage: String, putData: Any, whichThreadRequest: Int, whichMainRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(httpConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["putData"] = putData

        message.what = PUT_REQUEST
        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichThreadRequest
        message.arg2 = whichMainRequest
        message.sendToTarget()
    }

    // DELETE 메소드로 요청을 보낼 때
    fun serverDeleteRequest(serverPage: String, deleteData: Any, whichThreadRequest: Int, whichMainRequest: Int) {
        val messageObject = HashMap<String, Any>()
        val message = Message.obtain(httpConnectionThread.getHandler())

        messageObject["serverPage"] = serverPage
        messageObject["deleteData"] = deleteData

        message.what = DELETE_REQUEST
        message.obj = messageObject
        // mainHandler msg.what 의 값
        message.arg1 = whichThreadRequest
        message.arg2 = whichMainRequest
        message.sendToTarget()
    }
}