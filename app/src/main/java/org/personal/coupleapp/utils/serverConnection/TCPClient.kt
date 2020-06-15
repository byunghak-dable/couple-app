package org.personal.coupleapp.utils.serverConnection

import android.text.BoringLayout
import android.util.Log
import java.io.*
import java.lang.Exception
import java.net.Socket

class TCPClient(private val serverName: String, private val serverPort: Int) {

    private val TAG = javaClass.name

    private var socket: Socket? = null
    private var serverInPut: InputStream? = null
    private var serverOutPut: OutputStream? = null
    var bufferedReader: BufferedReader? = null

    // 서버와 연결을 확인하는 메소드
    fun connect(): Boolean {
        try {
            socket = Socket(serverName, serverPort)
            serverOutPut = socket!!.getOutputStream()
            serverInPut = socket!!.getInputStream()
            bufferedReader = BufferedReader(InputStreamReader(serverInPut!!))

        } catch (e: IOException) {
            e.printStackTrace()
            Log.i(TAG, "TCPClient connect : IOException 발생")
        }
        return true
    }

    // 커플 ID - 나의 ID - 이름 - 이미지 url - message
    fun writeMessage(message: String) {
        val writer = PrintWriter(serverOutPut!!)
        writer.println(message)
        writer.flush()
    }

    fun readMessage(): String? {
        if (!socket!!.isClosed) {
            val response: String? = bufferedReader?.readLine()
            if (response != null) {
                Log.i(TAG, "제대로 : $response")
                Log.i(TAG, "메시지 전송 완료")
            }
            return response
        }
        return null
    }

    fun socketClose() {
        Log.i(TAG, socket?.isClosed.toString())
        serverInPut?.close()
        serverOutPut?.close()
        bufferedReader?.close()
        socket?.close()
        Log.i(TAG, socket?.isClosed.toString())
    }
}

