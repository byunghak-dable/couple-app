package org.personal.coupleapp.utils.serverConnection

import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class HTTPConnection(private val requestUrl: String) {

    private val TAG = javaClass.name

    // 서버에서 json 객체로 인코딩해서 전달받은 jsonString
    private lateinit var jsonString: String

    // GET 메소드 사용, 서버로부터 데이터를 받아올 때 사용
    fun getRequest(): String {
        // HTTPRequest 클래스에서 string 형식으로 requestUrl 를 받아온다
        val url = URL(requestUrl)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Content-Type", "application/json; utf-8")
        urlConnection.setRequestProperty("Accept", "application/json")

        try {
            //TODO: use 에 관련되서 보다 공부가 필요
            jsonString = urlConnection.inputStream.bufferedReader().use { it.readText() }
            Log.i(TAG, jsonString)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(TAG, "getRequest inputStream 문제 발생")
        } finally {
            // 에러가 발생하더라도 openConnection 으로 연결한 connection 닫기
            urlConnection.disconnect()
        }
        return jsonString
    }

    // POST 메소드 사용, 서버로 데이터를 전송하고 데이터를 받아올 때 사용
    fun postRequest(postJsonString: String): String {
        // HTTPRequest 클래스에서 string 형식으로 requestUrl 를 받아온다
        val url = URL(requestUrl)
        val urlConnection = url.openConnection() as HttpURLConnection
        val outputStream: DataOutputStream
        Log.i(TAG, postJsonString)
        val postDataByteArray: ByteArray = postJsonString.toByteArray(StandardCharsets.UTF_8)

        urlConnection.requestMethod = "POST"
        // 서버로 부터 쓰기 설정
        urlConnection.doOutput = true
        urlConnection.setRequestProperty("Content-Type", "application/json; utf-8")
        urlConnection.setRequestProperty("Accept", "application/json")


        try {
            // 클라이언트에서 서버로 데이터 전송
            outputStream = DataOutputStream(urlConnection.outputStream)
            outputStream.write(postDataByteArray)
            outputStream.flush()

            // 서버에서 클라이언트로 데이터 받기
            jsonString = urlConnection.inputStream.bufferedReader().use { it.readText() }
            Log.i(TAG, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(TAG, "postRequest inputStream or OutputStream 문제 발생")
        } finally {
            // 에러가 발생하더라도 openConnection 으로 연결한 connection 닫기
            urlConnection.disconnect()
        }
        return jsonString
    }
}