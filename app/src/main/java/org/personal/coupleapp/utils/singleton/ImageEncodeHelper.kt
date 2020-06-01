package org.personal.coupleapp.utils.singleton

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream


object ImageEncodeHelper {

    private val TAG = javaClass.name

    // String 을 Bitmap 으로 변환
    fun stringToBitmap(encodedString: String?): Bitmap? {
        return try {
            val encodeByte: ByteArray = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(TAG, "String 에서 Bitmap 으로 변환 중 문제 발생")
            return null
        }
    }

    //Bitmap 을 String 형으로 변환
    fun bitmapToString(bitmap: Bitmap?): String? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 70, byteArrayOutputStream)
        val bytes: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }


     // Bitmap 을 byte 배열로 변환
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}