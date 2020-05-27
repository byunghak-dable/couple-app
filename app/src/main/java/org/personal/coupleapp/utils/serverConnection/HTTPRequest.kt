package org.personal.coupleapp.utils.serverConnection

import org.personal.coupleapp.data.SingleUserData

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
        val stringUrl = serverDomain + serverPage
        hTTPConnection = HTTPConnection(stringUrl)
        val jsonString: String = hTTPConnection.getRequest()

        return returnDataList
    }

    // 서버에 posting 을 하거나 하나의 value 만을 받을 때 사용
    override fun postToServer(postJsonString: String): String {
        val stringUrl = serverDomain + serverPage
        hTTPConnection = HTTPConnection(stringUrl)
        val jsonString = hTTPConnection.postRequest(postJsonString)


        return jsonString.replace("\"", "")
    }
}