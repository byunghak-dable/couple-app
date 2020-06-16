package org.personal.coupleapp.utils.serverConnection

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prolificinteractive.materialcalendarview.CalendarDay
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.personal.coupleapp.data.*
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class HTTPRequest(private val serverPage: String) : HTTPOutPut {

    companion object {
        const val POST = 1
        const val PUT = 2
    }

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

    // 자신의 프로파일 정보를 반환하는 메소드
    override fun getProfileFromServer(): ProfileData {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        Log.i("이미지 에러", jsonString)
        val profileData = gson.fromJson(jsonString, ProfileData::class.java)
        val profileImageBitmap = ImageEncodeHelper.getServerImage(profileData.profile_image.toString())
        profileData.profile_image = profileImageBitmap
        return profileData
    }

    // 스토리 정보를 반환하는 메소드
    override fun getStoryFromServer(): ArrayList<StoryData>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()

        Log.i(TAG, "getStoryFromServer : $jsonString")

        return if (jsonString != "false") {
            gson.fromJson(jsonString, object : TypeToken<ArrayList<StoryData>>() {}.type)
        } else {
            null
        }
    }

    // 커플 프로파일 정보를 반환하는 메소드
    override fun getCoupleProfile(): HashMap<String, ProfileData> {
        val coupleProfile = HashMap<String, ProfileData>()
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        val jsonObject = JSONObject(jsonString)

        val senderProfile = jsonObject.get("senderProfile") as JSONArray
        val receiverProfile = jsonObject.get("receiverProfile") as JSONArray

        coupleProfile["senderProfile"] = gson.fromJson(senderProfile[0].toString(), ProfileData::class.java)
        coupleProfile["receiverProfile"] = gson.fromJson(receiverProfile[0].toString(), ProfileData::class.java)

        return coupleProfile
    }

    // 일정 정보를 받아 캘린더에 보여주는 메소드
    override fun getPlanData(): HashMap<String, Any>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        var returnedData: HashMap<String, Any>? = null

        // 데이터베이스에 데이터가 있다면
        if (jsonString != "false") {
            returnedData = HashMap()
            val monthPlanList: ArrayList<PlanData> = gson.fromJson(jsonString, object : TypeToken<ArrayList<PlanData>>() {}.type)
            val dayPlanList = ArrayList<PlanData>()
            val dates = ArrayList<CalendarDay>()

            val todayCalendar = Calendar.getInstance()
            val planCalendar = Calendar.getInstance()
            var startTime: Long

            // 캘린더에 일정을 점으로 보여줄 dates 를 구성
            monthPlanList.forEach {
                startTime = it.start_time
                planCalendar.timeInMillis = startTime
                dates.add(CalendarDay.from(planCalendar))

                if (todayCalendar[Calendar.DAY_OF_MONTH] == planCalendar[Calendar.DAY_OF_MONTH]) {
                    dayPlanList.add(it)
                    Log.i(TAG, "캘린더 테스트 : ${todayCalendar[Calendar.DAY_OF_MONTH]}")
                    Log.i(TAG, "캘린더 테스트 : ${planCalendar[Calendar.DAY_OF_MONTH]}")
                    Log.i(TAG, "캘린더 테스트 : $dayPlanList")

                }
            }
            returnedData["monthPlanList"] = monthPlanList
            returnedData["dayPlanList"] = dayPlanList
            returnedData["dates"] = dates
        }
        return returnedData
    }

    // 오픈 채팅방 목록을 불러오는 메소드
    override fun getOpenChatList(): ArrayList<OpenChatRoomData>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()

        Log.i(TAG, "getOpenChatList : $jsonString")

        return if (jsonString != "false") {
            gson.fromJson(jsonString, object : TypeToken<ArrayList<OpenChatRoomData>>() {}.type)
        } else {
            null
        }
    }

    // 채팅 데이터를 불러오는 메소드
    override fun getChatHistory(): ArrayList<ChatData>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()

        Log.i(TAG, "getChatHistory : $jsonString")

        return if (jsonString != "false") {
            gson.fromJson(jsonString, object : TypeToken<ArrayList<ChatData>>() {}.type)
        } else {
            null
        }
    }

    // 앨범 폴더를 가져오는 메소드
    override fun getAlbumFolder(): ArrayList<AlbumFolderData>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        val responseJsonObject = JSONObject(jsonString)
        val httpStatus = responseJsonObject.getInt("httpStatus")
        val albumFolder = responseJsonObject.get("albumData").toString()

        var returnedData: ArrayList<AlbumFolderData>? = null

        if (httpStatus == 200) {
            if (albumFolder != "false") {
                returnedData = gson.fromJson(albumFolder, object : TypeToken<ArrayList<AlbumFolderData>>() {}.type)
            }
        }
        return returnedData
    }

    override fun getAlbumImages(): ArrayList<AlbumGalleryData>? {
        val gson = Gson()
        val jsonString: String = hTTPConnection.getRequest()
        Log.i(TAG, "http test: $jsonString")
        val responseJsonObject = JSONObject(jsonString)
        val httpStatus = responseJsonObject.getInt("httpStatus")
        val albumImages = responseJsonObject.get("imagesData").toString()

        var returnedData: ArrayList<AlbumGalleryData>? = null

        if (httpStatus == 200) {
            if (albumImages != "false") {
                returnedData = gson.fromJson(albumImages, object : TypeToken<ArrayList<AlbumGalleryData>>() {}.type)
            }
        }
        return returnedData
    }

    //------------------ POST 관련 메소드 모음 ------------------

    // 서버에 posting 을 하거나 하나의 value 만을 받을 때 사용
    override fun postMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.postRequest(postJsonString)

        return jsonString.replace("\"", "")
    }

    override fun signIn(postJsonString: String): ProfileData? {
        val profileData: ProfileData?
        val gson = Gson()
        val jsonString = hTTPConnection.postRequest(postJsonString)

        profileData = if (jsonString != "false") {
            gson.fromJson(jsonString, ProfileData::class.java)
        } else {
            null
        }

        return profileData
    }

    // 채팅 메시지를 저장을 요청하는 메소드
    override fun postChatToServer(chatData: ChatData, what: String): String? {
        val jsonString: String
        val gson = Gson()
        val postJson: String = gson.toJson(chatData)

        // gson 으로 변환한 json 에 어떤 작업을 하는지 알려주는 what 요소 추가
        val jsonObject = JSONObject(postJson)
        jsonObject.put("what", what)

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.postRequest(jsonObject.toString())
        Log.i(TAG, jsonString)

        return jsonString
    }

    // 오픈 채팅방을 만들어 서버에 저장을 요청하는 메소드
    override fun postOpenChatRoom(postData: HashMap<*, *>, what: String): String? {
        val jsonString: String
        val gson = Gson()
        val postJson: String
        val roomData = postData["openChatRoomData"] as OpenChatRoomData
        val base64Image = ImageEncodeHelper.bitmapToString(roomData.cover_image as Bitmap)

        // 변환한 이미지를 다시 선언하여 Bitmap 에서 String 타입이 변환
        roomData.cover_image = base64Image
        postJson = gson.toJson(postData)

        // gson 으로 변환한 json 에 어떤 작업을 하는지 알려주는 what 요소 추가
        val jsonObject = JSONObject(postJson)
        jsonObject.put("what", what)


        Log.i(TAG, "image test : $jsonObject")

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.postRequest(jsonObject.toString())
        Log.i(TAG, jsonString)

        return jsonString.replace("\"", "")
    }

    // 유저가 오픈 채팅방에 참여하는 메소드
    override fun postOpenChatUser(openChatUserData: OpenChatUserData, what: String): String? {
        val jsonString: String
        val gson = Gson()
        val postJson = gson.toJson(openChatUserData)
        val jsonObject = JSONObject(postJson).apply {
            put("what", what)
        }

        jsonString = hTTPConnection.postRequest(jsonObject.toString())
        return jsonString
    }

    // 앨범 폴더를 추가하는 메소드
    override fun postAlbumFolder(albumFolderData: AlbumFolderData, what: String): Int? {
        val responseCode: String
        val responseJsonObject: JSONObject
        val gson = Gson()
        val postJson = gson.toJson(albumFolderData)
        val postJsonObject = JSONObject(postJson).apply {
            put("what", what)
        }

        responseCode = hTTPConnection.postRequest(postJsonObject.toString())
        responseJsonObject = JSONObject(responseCode)
        return responseJsonObject.getInt("httpStatus")
    }

    // 앨범 사진들을 추가하는 메소드
    override fun postAlbumImages(galleryList: ArrayList<AlbumGalleryData>, what: String): Int? {
        val responseCode: String
        val responseJsonObject: JSONObject
        val postJson: String
        val gson = Gson()
        val galleryJsonObject = JSONObject()
        val galleryJsonArray: JSONArray

        galleryList.forEach {
            it.image_url = ImageEncodeHelper.bitmapToString(it.image_url as Bitmap)
        }
        postJson = gson.toJson(galleryList)

        galleryJsonArray = JSONArray(postJson)
        galleryJsonObject.put("what", what)
        galleryJsonObject.put("galleryImages", galleryJsonArray)

        responseCode = hTTPConnection.postRequest(galleryJsonObject.toString())
        responseJsonObject = JSONObject(responseCode)
        return responseJsonObject.getInt("httpStatus")
    }

    //------------------ PUT 관련 메소드 모음 ------------------

    override fun putMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.putRequest(postJsonString)
        Log.i(TAG, "put, delete 체크 : $jsonString ")
        return jsonString.replace("\"", "")
    }

    // 프로파일 정보를 서버로 보내는 메소드
    override fun putProfileToServer(profileData: ProfileData): ProfileData {
        val returnedProfileData: ProfileData
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
        returnedProfileData = gson.fromJson(jsonString, ProfileData::class.java)
        return returnedProfileData
    }

    //------------------ DELETE 관련 메소드 모음 ------------------
    // 삭제하는 메소드
    override fun deleteMethodToServer(postJsonString: String): String {
        val jsonString = hTTPConnection.deleteRequest(postJsonString)
        return jsonString.replace("\"", "")
    }

    //------------------ PUT, POST 모두 관여하는 메소드 모음 ------------------
    override fun handleStoryInServer(method: Int, storyData: StoryData, what: String): String? {
        var jsonString: String? = null
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
        when (method) {
            POST -> {
                jsonString = hTTPConnection.postRequest(jsonObject.toString())
            }
            PUT -> {
                jsonString = hTTPConnection.putRequest(jsonObject.toString())
            }
        }
        return jsonString
    }

    override fun handlePlanInServer(method: Int, planData: PlanData, what: String): String {
        val jsonString: String
        val gson = Gson()
        val postJson: String = gson.toJson(planData)

        // gson 으로 변환한 json 에 어떤 작업을 하는지 알려주는 what 요소 추가
        val jsonObject = JSONObject(postJson)
        jsonObject.put("what", what)

        // 결과를 받는다(성공 여부)
        jsonString = hTTPConnection.postRequest(jsonObject.toString())
        Log.i(TAG, jsonString)

        return jsonString
    }
}