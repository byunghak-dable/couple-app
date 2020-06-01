package org.personal.coupleapp.utils.serverConnection

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.SingleUserData
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper

class HTTPRequest(private val serverPage: String) : HTTPOutPut {

    private val TAG = javaClass.name

    // 서버 domain : ip 주소는 https redirect 설정이 되어 있어 aws domain 사용
    private val serverDomain = "http://ec2-13-125-99-215.ap-northeast-2.compute.amazonaws.com/"

    // 서버와 연결을 관리하는 클래스
    private lateinit var hTTPConnection: HTTPConnection

    // TODO : 실제 데이터 handle 해야 함
    override fun fetchFromServer(): List<SingleUserData> {
        val returnDataList = ArrayList<SingleUserData>()
        // domain 과 페이지를 통해 url 완성
        hTTPConnection = HTTPConnection(serverDomain + serverPage)
        val jsonString: String = hTTPConnection.getRequest()

        return returnDataList
    }

    // 서버에 posting 을 하거나 하나의 value 만을 받을 때 사용
    override fun postToServer(postJsonString: String): String {
        hTTPConnection = HTTPConnection(serverDomain + serverPage)
        val jsonString = hTTPConnection.postRequest(postJsonString)

        return jsonString.replace("\"", "")
    }

    // 프로파일 정보를 서버로 보내는 메소드
    override fun postProfileToServer(profileData: ProfileData): String {
        hTTPConnection = HTTPConnection(serverDomain + serverPage)
        val jsonString:String
        val gson = Gson()
        val postJson: String
        // 비트맵 이미지를 base64 로 변환
        val base64Image = ImageEncodeHelper.bitmapToString(profileData.profile_image as Bitmap)

        // 변환한 이미지를 다시 선언하여 Bitmap 에서 String 타입이 변환
        profileData.profile_image = base64Image
        postJson = gson.toJson(profileData)

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.postRequest(postJson)
        Log.i(TAG, jsonString)
        return jsonString
    }
}