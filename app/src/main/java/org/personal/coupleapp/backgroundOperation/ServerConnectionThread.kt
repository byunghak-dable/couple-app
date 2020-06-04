package org.personal.coupleapp.backgroundOperation

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.utils.serverConnection.HTTPRequest
import org.personal.coupleapp.utils.serverConnection.HTTPRequest.Companion.POST
import org.personal.coupleapp.utils.serverConnection.HTTPRequest.Companion.PUT

class ServerConnectionThread(name: String?, private val mainHandler: Handler) : HandlerThread(name) {

    // msg.what, msg.arg1 의 value 값 선언
    companion object {
        const val GET_REQUEST = 1
        const val POST_REQUEST = 2
        const val PUT_REQUEST = 3
        const val REQUEST_DELETE = 4


        // get 메소드 관련
        const val REQUEST_SIMPLE_GET_METHOD = 1
        const val REQUEST_STORY_DATA = 2
        const val REQUEST_PROFILE_INFO = 3
        const val REQUEST_COUPLE_PROFILE = 4
        const val REQUEST_PLAN_DATA = 5

        // post 메소드 관련
        const val REQUEST_SIMPLE_POST_METHOD = 1
        const val REQUEST_INSERT_STORY_DATA = 2
        const val REQUEST_INSERT_PLAN_DATA = 3

        // put 메소드 관련
        const val REQUEST_PROFILE_UPDATE = 1
        const val REQUEST_UPDATE_STORY_DATA = 2
        const val REQUEST_UPDATE_PLAN_DATA = 3

        // delete 메소드 관련
        const val DELETE_METHOD = 1
    }

    private val TAG = javaClass.name

    private lateinit var handler: Handler

    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        Log.i("thread-test", "onLooperPrepared")

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {

                // mainHandler 의 msg.what 값은 msg.arg1 에서 추출
                val whichMessage = msg.arg1
                // 어떤 HTTPRequest 메소드를 사용할 지 정함
                val whichRequest: Int? = msg.arg2

                when (msg.what) {

                    // 서버로부터 데이터를 받아오고 결과를 UIHandler 로 전송
                    GET_REQUEST -> {
                        val requestPage = msg.obj.toString()
                        val httpRequest = HTTPRequest(requestPage)
                        val message = Message.obtain(mainHandler)

                        when (whichRequest) {
                            // 간단한 GET request
                            REQUEST_SIMPLE_GET_METHOD -> {
                                message.obj = httpRequest.getMethodToServer()
                            }
                            REQUEST_STORY_DATA -> {
                                message.obj = httpRequest.getStoryFromServer()
                            }
                            REQUEST_PROFILE_INFO -> {
                                message.obj = httpRequest.getProfileFromServer()
                            }

                            REQUEST_COUPLE_PROFILE -> {
                                message.obj = httpRequest.getCoupleProfile()
                            }

                            REQUEST_PLAN_DATA -> {
                                message.obj = httpRequest.getPlanData()
                            }
                        }
                        message.what = whichMessage
                        message.sendToTarget()
                    }

                    // 서버에 데이터를 전송하고 서버에서 받은 결과를 UIHandler 로 전송
                    POST_REQUEST -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)
                        val postData: Any

                        when (whichRequest) {
                            // 간단한 데이터(메인 스레드에서 jsonString 으로 만들어서 보내는 경우)
                            REQUEST_SIMPLE_POST_METHOD -> {
                                postData = msgObjHashMap["postData"].toString()
                                message.obj = httpRequest.postMethodToServer(postData)
                            }
                            REQUEST_INSERT_STORY_DATA -> {
                                postData = msgObjHashMap["postData"] as StoryData
                                message.obj = httpRequest.handleStoryInServer(POST, postData, "addStoryData")
                            }

                            REQUEST_INSERT_PLAN_DATA -> {
                                postData = msgObjHashMap["postData"] as PlanData
                                message.obj = httpRequest.handlePlanInServer(POST, postData, "addPlanData")
                            }
                        }
                        message.what = whichMessage
                        message.sendToTarget()
                    }

                    PUT_REQUEST -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)
                        val putData: Any
                        Log.i(TAG, "잘오는 지 확인 $whichRequest")
                        when (whichRequest) {
                            // 프로필 수정을 하는 경우
                            REQUEST_PROFILE_UPDATE -> {
                                putData = msgObjHashMap["putData"] as ProfileData
                                Log.i(TAG, "테스트 $putData")
                                message.obj = httpRequest.putProfileToServer(putData)
                            }
                            // 스토리 데이터를 수정하는 경우
                            REQUEST_UPDATE_STORY_DATA -> {
                                putData = msgObjHashMap["putData"] as StoryData
                                message.obj = httpRequest.handleStoryInServer(PUT, putData, "modifyStoryData")
                            }

                            REQUEST_UPDATE_PLAN_DATA -> {
                                putData = msgObjHashMap["putData"] as PlanData
                                message.obj = httpRequest.handlePlanInServer(PUT, putData, "modifyPlanData")
                            }
                        }
                        message.what = whichMessage
                        message.sendToTarget()
                    }

                    REQUEST_DELETE -> {
                        // HashMap 으로 보낸 msg.obj 객체 캐스팅
                        val msgObjHashMap = msg.obj as HashMap<*, *>
                        val serverPage = msgObjHashMap["serverPage"].toString()
                        val httpRequest = HTTPRequest(serverPage)
                        val message = Message.obtain(mainHandler)
                        val deleteData: Any

                        when (whichRequest) {
                            DELETE_METHOD -> {
                                deleteData = msgObjHashMap["deleteData"].toString()
                                message.obj = httpRequest.deleteMethodToServer(deleteData)
                            }
                        }
                        message.what = whichMessage
                        message.sendToTarget()
                    }
                }
            }
        }
    }

    fun getHandler(): Handler {
        return handler
    }
}