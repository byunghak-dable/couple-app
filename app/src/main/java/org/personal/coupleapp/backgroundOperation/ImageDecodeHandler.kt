package org.personal.coupleapp.backgroundOperation

import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ImageView
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import java.lang.ref.WeakReference


class ImageDecodeHandler(imageView: ImageView, private val imageList: ArrayList<Bitmap>): Handler() {

    companion object{
        const val SINGLE_IMAGE = 1
        const val MULTIPLE_IMAGE = 2
    }

    private var imageViewWeakReference: WeakReference<ImageView>? = WeakReference(imageView)



    // 백그라운드에서 bitmap 으로 decoding 한 이미지를 받아 UI 에 갱신하는 작업
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        val imageView = imageViewWeakReference!!.get()
        when(msg.what) {

            // 이미지 하나만을 올릴 때 리스트를 초기화한다
            SINGLE_IMAGE -> {
                imageList.clear()
                imageList.add(msg.obj as Bitmap)
                imageView?.setImageBitmap(msg.obj as Bitmap)
            }

            // 이미지 여러 개 올려야할 때(스토리) 리스트에 이미지를 추가한다
            MULTIPLE_IMAGE -> {
                imageList.add(msg.obj as Bitmap)
                imageView?.setImageBitmap(msg.obj as Bitmap)
            }

        }

    }
}