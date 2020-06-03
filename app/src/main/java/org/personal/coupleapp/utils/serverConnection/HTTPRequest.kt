package org.personal.coupleapp.utils.serverConnection

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper

class HTTPRequest(private val serverPage: String) : HTTPOutPut {

    private val TAG = javaClass.name

    // 서버 domain : ip 주소는 https redirect 설정이 되어 있어 aws domain 사용
    private val serverDomain = "http://ec2-13-125-99-215.ap-northeast-2.compute.amazonaws.com/"

    // 서버와 연결을 관리하는 클래스
    // domain 과 페이지를 통해 url 완성
    private val hTTPConnection: HTTPConnection = HTTPConnection(serverDomain + serverPage)

    //------------------ GET 관련 메소드 모음 ------------------
    override fun getMethodToServer(): String {
        val jsonString: String = hTTPConnection.getRequest()

        return jsonString.replace("\"", "")
    }

    override fun getProfileFromServer(): ProfileData {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        val profileData = gson.fromJson(jsonString, ProfileData::class.java)
        val profileImageBitmap = ImageEncodeHelper.getServerImage(profileData.profile_image.toString())
        profileData.profile_image = profileImageBitmap
        return profileData
    }

    override fun getStoryFromServer(): ArrayList<StoryData>? {
        val gson = Gson()

        // 결과를 받는다(성공 여부)
        val jsonString: String = hTTPConnection.getRequest()

        return if (jsonString != "false") {
            gson.fromJson(jsonString, object : TypeToken<ArrayList<StoryData>>() {}.type)
        } else {
            null
        }
    }

    //------------------ POST 관련 메소드 모음 ------------------
    // 서버에 posting 을 하거나 하나의 value 만을 받을 때 사용
    override fun postMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.postRequest(postJsonString)

        return jsonString.replace("\"", "")
    }

    override fun postStoryToServer(storyData: StoryData, what: String): String {
        val jsonString: String
        val gson = Gson()
        val postJson: String
        val base64ImageList: ArrayList<String?> = ArrayList()

        // 비트맵 이미지를 base64 로 변환
        storyData.photo_path.forEach {
            base64ImageList.add(ImageEncodeHelper.bitmapToString(it as Bitmap))
        }

        // 변환한 이미지를 다시 선언하여 Bitmap 에서 String 타입이 변환
        storyData.photo_path = base64ImageList as ArrayList<Any>
        postJson = gson.toJson(storyData)

        // gson 으로 변환한 json 에 어떤 작업을 하는지 알려주는 what 요소 추가
        val jsonObject = JSONObject(postJson)
        jsonObject.put("what", what)

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.postRequest(jsonObject.toString())
        Log.i(TAG, jsonString)
        return jsonString
    }

    //------------------ PUT 관련 메소드 모음 ------------------

    override fun putMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.putRequest(postJsonString)
        Log.i(TAG, "put, delete 체크 : $jsonString ")
        return jsonString.replace("\"", "")
    }

    // 프로파일 정보를 서버로 보내는 메소드
    override fun putProfileToServer(profileData: ProfileData): String {
        val jsonString: String
        val gson = Gson()
        val postJson: String
        // 비트맵 이미지를 base64 로 변환
        val base64Image = ImageEncodeHelper.bitmapToString(profileData.profile_image as Bitmap)

        // 변환한 이미지를 다시 선언하여 Bitmap 에서 String 타입이 변환
        profileData.profile_image = base64Image
        postJson = gson.toJson(profileData)

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.putRequest(postJson)
        Log.i(TAG, jsonString)
        return jsonString
    }

    //------------------ DELETE 관련 메소드 모음 ------------------
    // 삭제하는 메소드
    override fun deleteMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.deleteRequest(postJsonString)
        return jsonString.replace("\"", "")
    }
}