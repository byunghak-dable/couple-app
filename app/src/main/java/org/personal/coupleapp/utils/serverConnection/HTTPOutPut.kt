package org.personal.coupleapp.utils.serverConnection

import org.personal.coupleapp.data.UserData

// HTTPRequest 에 implement, 서버와의 통신 후 결과를 반환하는 메소드 포함
interface HTTPOutPut {

    // TODO: 각각의 data 리스트를 반환할 지 jsonString 을 반환할 지 정해야 함.
    fun fetchFromServer(): List<UserData>
    fun postToServer(postJsonString: String): String
}