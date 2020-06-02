package org.personal.coupleapp.backgroundOperation

import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.widget.ImageView
import java.lang.ref.WeakReference


class ImageDecodeHandler(imageView: ImageView, private val imageList: ArrayList<Bitmap>) : Handler() {

    private var imageViewWeakReference: WeakReference<ImageView>? = WeakReference(imageView)

    // 백그라운드에서 bitmap 으로 decoding 한 이미지를 받아 UI 에 갱신하는 작업
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        val imageView = imageViewWeakReference!!.get()

        imageList.clear()
        imageList.add(msg.obj as Bitmap)
        imageView?.setImageBitmap(msg.obj as Bitmap)
    }
}