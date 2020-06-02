package org.personal.coupleapp.utils.singleton

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL


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

    // 서버나 외부 URL 로부터 이미지를 불러오는 메소드
    fun getServerImage(imageUrl: String): Bitmap? {
        //서버에 올려둔 이미지 URL
        val url = URL(imageUrl)
        val urlConnection = url.openConnection() as HttpURLConnection
        var bitmap: Bitmap? = null

        urlConnection.connect() //연결된 곳에 접속할 때 (connect() 호출해야 실제 통신 가능함)

        try {
            val inputStream = urlConnection.inputStream //inputStream 값 가져오기
            bitmap = BitmapFactory.decodeStream(inputStream) // Bitmap으로 반환
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(TAG, "IO 문제 발생")
        } finally {
            // 에러가 발생하더라도 openConnection 으로 연결한 connection 닫기
            urlConnection.disconnect()
        }
        return bitmap
    }
}