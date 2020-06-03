package org.personal.coupleapp.utils.serverConnection

import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.StoryData

// HTTPRequest 에 implement, 서버와의 통신 후 결과를 반환하는 메소드 포함
interface HTTPOutPut {

    // GET 메소드
    fun getMethodToServer(): String
    fun getProfileFromServer(): ProfileData
    fun getStoryFromServer(): ArrayList<StoryData>?

    // POST 메소드
    fun postMethodToServer(postJsonString: String): String
    fun postStoryToServer(storyData: StoryData, what: String): String

    // PUT 메소드
    fun putMethodToServer(postJsonString: String): String
    fun putProfileToServer(profileData: ProfileData): String

    // DELETE 메소드
    fun deleteMethodToServer(postJsonString: String): String
}