package org.personal.coupleapp.utils.singleton

import android.os.Message
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread

object HandlerMessageHelper {

    private val TAG = javaClass.name

    // decodingThread 에게 이미지 uri 를 보내는 메소드
    fun decodeImage(imageDecodeThread: ImageDecodeThread, targetImageData: Any?, whichMessage: Int, mainMessage: Int) {
        val message: Message = Message.obtain(imageDecodeThread.getHandler())
        message.obj = targetImageData
        message.what = whichMessage
        message.arg1 = mainMessage
        message.sendToTarget()
    }
}