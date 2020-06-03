package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.RequiresApi
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import java.io.FileNotFoundException
import java.io.IOException

@Suppress("UNCHECKED_CAST")
class ImageDecodeThread(name: String?, private val context: Context, private val mainHandler: Handler) : HandlerThread(name) {

    companion object {
        const val DECODE_INTO_BITMAP = 1
        const val DECODE_INTO_MULTIPLE_BITMAP = 2
        const val DECODE_URL_TO_BITMAP = 3
    }

    private val TAG = javaClass.name

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        handler = object : Handler() {

            @RequiresApi(Build.VERSION_CODES.P)
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                val whichMessage: Int = msg.arg1

                when (msg.what) {

                    // 하나의 이미지(Uri)를 bitmap 으로 변환하는 메시지
                    DECODE_INTO_BITMAP -> {
                        val imageUri = msg.obj as Uri
                        val contentResolver: ContentResolver = context.contentResolver
                        val imageSource = ImageDecoder.createSource(contentResolver, imageUri)

                        try {
                            val bitmap = ImageDecoder.decodeBitmap(imageSource)
                            val message = Message.obtain()
                            message.what = whichMessage
                            message.obj = bitmap
                            mainHandler.sendMessage(message)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    // 여러 이미지(Uri)를 bitmap 으로 변환하는 메시지
                    DECODE_INTO_MULTIPLE_BITMAP -> {

                        val imageUriList = msg.obj as ClipData?
                        val contentResolver: ContentResolver = context.contentResolver
                        val bitmapList = ArrayList<Bitmap>()
                        val message = Message.obtain()
                        Log.i(TAG, imageUriList.toString())
                        if (imageUriList != null) {
                            for (i in 0 until imageUriList.itemCount) {
                                val imageUri = imageUriList.getItemAt(i).uri

                                try {

                                    Log.i(TAG, imageUri.toString())
                                    val imageSource = ImageDecoder.createSource(contentResolver, imageUri)
                                    val bitmap = ImageDecoder.decodeBitmap(imageSource)
                                    Log.i(TAG, "bitmap$bitmap")
                                    bitmapList.add(bitmap)
                                } catch (e: FileNotFoundException) {
                                    e.printStackTrace()
                                }
                            }

                            message.what = whichMessage
                            message.obj = bitmapList
                            mainHandler.sendMessage(message)
                        }
                    }

                    DECODE_URL_TO_BITMAP -> {
                        val imageUrls = msg.obj as ArrayList<String>
                        val imageBitmaps = ArrayList<Bitmap?>()
                        val message = Message.obtain()

                        Log.i(TAG, "DECODE_URL_TO_BITMAP : $imageUrls")

                        imageUrls.forEach { imageBitmaps.add(ImageEncodeHelper.getServerImage(it)) }

                        message.what = whichMessage
                        message.obj = imageBitmaps
                        mainHandler.sendMessage(message)
                    }
                }
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}

