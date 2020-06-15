package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import org.personal.coupleapp.data.*
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.utils.serverConnection.HTTPRequest
import org.personal.coupleapp.utils.serverConnection.HTTPRequest.Companion.POST

class HTTPConnectionThread(name: String?, private val httpConnectionListener: HTTPConnectionListener) : HandlerThread(name) {

    // msg.what, msg.arg1 의 value 값 선언
    companion object {
        const val GET_REQUEST = 1
        const val POST_REQUEST = 2
        const val PUT_REQUEST = 3
        const val DELETE_REQUEST = 4


        // get 메소드 관련
        const val REQUEST_SIMPLE_GET_METHOD = 1
        const val REQUEST_STORY_DATA = 2
        const val REQUEST_PROFILE_INFO = 3
        const val REQUEST_COUPLE_PROFILE = 4
        const val REQUEST_PLAN_DATA = 5
        const val REQUEST_OPEN_CHAT_ROOM_LIST = 6
        const val REQUEST_CHAT_HISTORY = 7

        // post 메소드 관련
        const val REQUEST_SIMPLE_POST_METHOD = 1
        const val REQUEST_INSERT_STORY_DATA = 2
        const val REQUEST_INSERT_PLAN_DATA = 3
        const val REQUEST_SIGN_IN = 4
        const val REQUEST_INSERT_COUPLE_CHAT_DATA = 5
        const val REQUEST_INSERT_OPEN_CHAT_ROOM = 6
        const val REQUEST_INSERT_OPEN_CHAT_USER = 7
        const val REQUEST_INSERT_OPEN_CHAT_DATA = 8

        // put 메소드 관련
        const val REQUEST_PROFILE_UPDATE = 1
        const val REQUEST_UPDATE_STORY_DATA = 2
        const val REQUEST_UPDATE_PLAN_DATA = 3

        // delete 메소드 관련
        const val DELETE_FROM_SERVER = 1
    }

    private val TAG = javaClass.name

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        Log.i("thread-test", "onLooperPrepared")

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                // 메인에 보낼 데이터 저장하는 헤시 맵
                val httpRespondData = HashMap<String, Any?>()
                // 어떤 HTTPRequest 메소드를 사용할 지 정함
                val whichRequest: Int? = msg.arg1
                // 메인에서 어떤 메시지를 보낼지 정한다.
                httpRespondData["whichRespond"] = msg.arg2

                when (msg.what) {

                    // 서버로부터 데이터를 받아오고 결과를 UIHandler 로 전송
                    GET_REQUEST -> {
                        val requestPage = msg.obj.toString()
                        val httpRequest = HTTPRequest(requestPage)

                        when (whichRequest) {
                            // 간단한 GET request
                            REQUEST_SIMPLE_GET_METHOD -> {
                                httpRespondData["respondData"] = httpRequest.getMethodToServer()
                            }
                            REQUEST_STORY_DATA -> {
                                httpRespondData["respondData"] = httpRequest.getStoryFromServer()
                            }
                            REQUEST_PROFILE_INFO -> {
                                httpRespondData["respondData"] = httpRequest.getProfileFromServer()
                            }

                            REQUEST_COUPLE_PROFILE -> {
                                httpRespondData["respondData"] = httpRequest.getCoupleProfile()
                            }

                            REQUEST_PLAN_DATA -> {
                                httpRespondData["respondData"] = httpRequest.getPlanData()
                            }

                            REQUEST_OPEN_CHAT_ROOM_LIST -> {
                                httpRespondData["respondData"] = httpRequest.getOpenChatList()
                            }

                            REQUEST_CHAT_HISTORY -> {
                                httpRespondData["respondData"] = httpRequest.getChatHistory()
                            }
                        }
                    }

                    // 서버에 데이터를 전송하고 서버에서 받은 결과를 UIHandler 로 전송
                    POST_REQUEST -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val postData: Any

                        when (whichRequest) {
                            // 간단한 데이터(메인 스레드에서 jsonString 으로 만들어서 보내는 경우)
                            REQUEST_SIMPLE_POST_METHOD -> {
                                postData = msgObjHashMap["postData"].toString()
                                httpRespondData["respondData"] = httpRequest.postMethodToServer(postData)
                            }
                            REQUEST_INSERT_STORY_DATA -> {
                                postData = msgObjHashMap["postData"] as StoryData
                                httpRespondData["respondData"] = httpRequest.handleStoryInServer(POST, postData, "addStoryData")
                            }

                            REQUEST_INSERT_PLAN_DATA -> {
                                postData = msgObjHashMap["postData"] as PlanData
                                httpRespondData["respondData"] = httpRequest.handlePlanInServer(POST, postData, "addPlanData")
                            }
                            REQUEST_SIGN_IN -> {
                                postData = msgObjHashMap["postData"] as String
                                httpRespondData["respondData"] = httpRequest.signIn(postData)
                            }

                            REQUEST_INSERT_COUPLE_CHAT_DATA-> {
                                postData = msgObjHashMap["postData"] as ChatData
                                httpRespondData["respondData"] = httpRequest.postChatToServer(postData, "sendCoupleChat")
                            }

                            REQUEST_INSERT_OPEN_CHAT_ROOM -> {
                                postData = msgObjHashMap["postData"] as HashMap<*,*>
                                httpRespondData["respondData"] = httpRequest.postOpenChatRoom(postData, "addOpenChatRoom")
                            }

                            REQUEST_INSERT_OPEN_CHAT_USER -> {
                                postData = msgObjHashMap["postData"] as OpenChatUserData
                                httpRespondData["respondData"] = httpRequest.postOpenChatUser(postData, "addOpenChatUser")
                            }

                            REQUEST_INSERT_OPEN_CHAT_DATA -> {
                                postData = msgObjHashMap["postData"] as ChatData
                                httpRespondData["respondData"] = httpRequest.postChatToServer(postData, "sendOpenChat")
                            }
                        }
                    }

                    PUT_REQUEST -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val putData: Any

                        when (whichRequest) {
                            // 프로필 수정을 하는 경우
                            REQUEST_PROFILE_UPDATE -> {
                                putData = msgObjHashMap["putData"] as ProfileData
                                Log.i(TAG, "테스트 $putData")
                                httpRespondData["respondData"]  = httpRequest.putProfileToServer(putData)
                            }
                            // 스토리 데이터를 수정하는 경우
                            REQUEST_UPDATE_STORY_DATA -> {
                                putData = msgObjHashMap["putData"] as StoryData
                                httpRespondData["respondData"] = httpRequest.handleStoryInServer(HTTPRequest.PUT, putData, "modifyStoryData")
                            }

                            REQUEST_UPDATE_PLAN_DATA -> {
                                putData = msgObjHashMap["putData"] as PlanData
                                httpRespondData["respondData"] = httpRequest.handlePlanInServer(HTTPRequest.PUT, putData, "modifyPlanData")
                            }
                        }
                    }

                    DELETE_REQUEST -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val deleteData: Any

                        when (whichRequest) {
                            DELETE_FROM_SERVER -> {
                                deleteData = msgObjHashMap["deleteData"].toString()
                                httpRespondData["respondData"] =  httpRequest.deleteMethodToServer(deleteData)
                            }
                        }
                    }
                }
                httpConnectionListener.onHttpRespond(httpRespondData)
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}