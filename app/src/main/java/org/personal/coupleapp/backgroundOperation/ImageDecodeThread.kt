package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import java.io.IOException

class ImageDecodeThread(name: String?, private val context: Context, private val mainHandler: Handler) : HandlerThread(name) {

    companion object {
        const val DECODE_INTO_BITMAP = 1
    }

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        handler = object : Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when (msg.what) {

                    DECODE_INTO_BITMAP -> {
                        val imageUri = msg.obj as Uri
                        val whichRequest = msg.arg1
                        val contentResolver: ContentResolver = context.contentResolver
                        val imageSource = ImageDecoder.createSource(contentResolver, imageUri)

                        try {
                            val bitmap = ImageDecoder.decodeBitmap(imageSource)
                            val message = Message.obtain()
                            message.what = whichRequest
                            message.obj = bitmap
                            mainHandler.sendMessage(message)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}

