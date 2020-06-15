package org.personal.coupleapp.utils.serverConnection

import org.personal.coupleapp.data.*

// HTTPRequest 에 implement, 서버와의 통신 후 결과를 반환하는 메소드 포함
interface HTTPOutPut {

    // GET 메소드
    fun getMethodToServer(): String
    fun getProfileFromServer(): ProfileData
    fun getStoryFromServer(): ArrayList<StoryData>?
    fun getCoupleProfile(): HashMap<String, ProfileData>
    fun getPlanData(): HashMap<String, Any>?
    fun getOpenChatList(): ArrayList<OpenChatRoomData>?
    fun getChatHistory() : ArrayList<ChatData>?

    // POST 메소드
    fun postMethodToServer(postJsonString: String): String
    fun signIn(postJsonString: String): ProfileData?
    fun postChatToServer(chatData: ChatData, what:String): String?
    fun postOpenChatRoom(postData: HashMap<*, *>,  what: String) : String?
    fun postOpenChatUser(openChatUserData: OpenChatUserData, what: String) : String?

    // PUT 메소드
    fun putMethodToServer(postJsonString: String): String
    fun putProfileToServer(profileData: ProfileData): ProfileData?

    // DELETE 메소드
    fun deleteMethodToServer(postJsonString: String): String

    // POST 혹은 PUT 메소드
    fun handleStoryInServer(method: Int, storyData: StoryData, what: String): String?
    fun handlePlanInServer(method: Int, planData: PlanData, what: String): String?
}