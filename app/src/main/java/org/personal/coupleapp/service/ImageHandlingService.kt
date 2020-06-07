package org.personal.coupleapp.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.os.Message
import android.util.Log
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
import org.personal.coupleapp.interfaces.service.ImageHandlingListener

class ImageHandlingService : Service(), ImageHandlingListener {

    private val TAG = javaClass.name

    private val binder: IBinder = LocalBinder()

    private lateinit var imageDecodeThread: ImageDecodeThread
    private var imageHandlingListener: ImageHandlingListener? = null

    override fun onCreate() {
        super.onCreate()
        imageDecodeThread = ImageDecodeThread("ImageDecodeThread", this, this)
        imageDecodeThread.start()
        Log.i(TAG, "onCreate : 이미지 핸들링 스레드 시작")
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
        imageDecodeThread.looper.quit()
        Log.i(TAG, "onDestroy : 이미지 핸들링 스레드 종료")
    }

    fun setOnImageListener(listener: ImageHandlingListener) {
        imageHandlingListener = listener
    }

    inner class LocalBinder : Binder() {
        fun getService(): ImageHandlingService? {

            return this@ImageHandlingService
        }
    }

    override fun onSingleImage(bitmap: Bitmap) {
        imageHandlingListener!!.onSingleImage(bitmap)
    }

    override fun onMultipleImage(bitmapList: ArrayList<Bitmap?>) {
        imageHandlingListener!!.onMultipleImage(bitmapList)
    }


    // decodingThread 에게 이미지 uri 를 보내는 메소드
    fun decodeImage(targetImageData: Any?, whichMessage: Int) {
        val message: Message = Message.obtain(imageDecodeThread.getHandler())
        message.obj = targetImageData
        message.what = whichMessage
        message.sendToTarget()
    }
}