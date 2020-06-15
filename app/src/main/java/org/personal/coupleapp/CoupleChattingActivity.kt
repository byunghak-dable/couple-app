package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_couple_chatting.*
import org.personal.coupleapp.adapter.ChatAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_CHAT_HISTORY
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_COUPLE_CHAT_DATA
import org.personal.coupleapp.data.ChatData
import org.personal.coupleapp.interfaces.service.ChatListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.ChatService
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class CoupleChattingActivity : AppCompatActivity(), View.OnClickListener, ChatListener, HTTPConnectionListener {

    private val TAG = javaClass.name

    private val serverPage = "CoupleChatting"

    private lateinit var chatService: ChatService
    private lateinit var httpConnectionService: HTTPConnectionService

    private val SEND_MESSAGE_RESPOND = 1
    private val GET_CHAT_HISTORY = 2

    private val coupleChatList by lazy { ArrayList<ChatData>() }
    private val chatAdapter: ChatAdapter by lazy { ChatAdapter(this, userColumnID, coupleChatList) }

    val coupleID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString()) }
    val userColumnID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString()) }
    val userName by lazy { SharedPreferenceHelper.getString(this, getText(R.string.userName).toString()) }
    val profileImageUrl by lazy { SharedPreferenceHelper.getString(this, getText(R.string.profileImageUrl).toString()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_couple_chatting)
        setListener()
        buildRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        // 커플룸을 빠져나온다는 것을 서버에게 알린다
        chatService.sendMessage("exitCoupleChat")
        unbindService(chatConnection)
        unbindService(httpConnection)

        SharedPreferenceHelper.setInt(this, getText(R.string.joinedChatRoomNumber).toString(), 0)
    }

    private fun setListener() {
        sendBtn.setOnClickListener(this)
    }

    // 리사이클러 뷰 빌드
    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        chattingBoxRV.setHasFixedSize(true)
        chattingBoxRV.layoutManager = layoutManager
        chattingBoxRV.adapter = chatAdapter
    }

    // 바운드 서비스와 연결하는 메소드
    private fun startBoundService() {
        val startChatService = Intent(this, ChatService::class.java)
        val startHttpService = Intent(this, HTTPConnectionService::class.java)
        bindService(startChatService, chatConnection, BIND_AUTO_CREATE)
        bindService(startHttpService, httpConnection, BIND_AUTO_CREATE)
    }

    // GET 요청을 보낼 url 을 만드는 메소드
    private fun makeRequestUrl(what: String): String {
        return "$serverPage?what=$what&&roomID=$coupleID"
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sendBtn -> formMessageData(chattingInputED.text.toString())
        }
    }

    // 커플 ID - 나의 ID - 이름 - 이미지 url - message
    // 보낼 메시지를 봰는  
    private fun formMessageData(message: String) {
        val delimiter = "@"
        val messageData = "sendMessageToCouple$delimiter$message"
        val messageTime = CalendarHelper.getCurrentTime()
        val chatData = ChatData(coupleID, userColumnID, userName, profileImageUrl, message, messageTime)

        chatService.sendMessage(messageData)
        httpConnectionService.serverPostRequest(serverPage, chatData, REQUEST_INSERT_COUPLE_CHAT_DATA, SEND_MESSAGE_RESPOND)
        chattingInputED.text = null
        Log.i(TAG, messageData)
    }

    // chatService 바인드 서비스 인터페이스 메소드 : 서비스에 있는 리스너를 통해 메시지를 읽어 온다.
    // 커플 ID - 나의 ID - 이름 - 이미지 url - message
    // 메시지를 읽고 메시지의 정보를 split 으로 나눠 각각의 정보로 messageData 객체를 생성하고 추가
    override fun onReceiveChat(respond: String?) {
        val handler = Handler(Looper.getMainLooper())
        val delimiter = "@"
        val splitData = respond!!.split(delimiter)

        val coupleColumnID = parseInt(splitData[0])
        val userColumnID = parseInt(splitData[1])
        val senderName = splitData[2].replace("'", "")
        val profileImageUrl = splitData[3].replace("'", "")
        val message = splitData[4]
        val messageTime = CalendarHelper.getCurrentTime()

        // 채팅 데이터 객체 생성
        val coupleChatData = ChatData(coupleColumnID, userColumnID, senderName, profileImageUrl, message, messageTime)

        coupleChatList.add(coupleChatData)

        handler.post {
            chatAdapter.notifyItemInserted(coupleChatList.indexOf(coupleChatData))
            chattingBoxRV.scrollToPosition(chatAdapter.itemCount - 1)
        }
        Log.i(TAG, coupleChatList.toString())
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {

            // 채팅을 서버에 저장하고 결과를 받는 경우
            SEND_MESSAGE_RESPOND -> {
                val respondMessage = responseData["respondData"].toString()
                Log.i(TAG, "onHttpRespond respond :$respondMessage")
            }

            GET_CHAT_HISTORY -> {
                Log.i(TAG, "http 테스트 : ${responseData["respondData"]}")
                if (responseData["respondData"] == null) {
                    Log.i(TAG, "GET_CHAT_HISTORY : 채팅 데이터가 없음")
                } else {
                    val fetchedChatHistory = responseData["respondData"] as ArrayList<ChatData>
                    fetchedChatHistory.forEach { coupleChatList.add(it) }
                    handler.post{chatAdapter.notifyDataSetChanged()}
                }
            }
        }
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    private val chatConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: ChatService.LocalBinder = service as ChatService.LocalBinder
            chatService = binder.getService()!!
            chatService.setOnChatRespondListener(this@CoupleChattingActivity)
            sendInitDataToServer()
            joinCoupleChat()
            SharedPreferenceHelper.setInt(this@CoupleChattingActivity, getText(R.string.joinedChatRoomNumber).toString(), coupleID)
            Log.i(TAG, "Bound Service Connection : Connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "Bound Service Connection : 바운드 서비스 종류")
        }

        // 서버 클라이언트 소켓의 변수를 지정하기 위해 접속하자마자 데이터 전송
        private fun sendInitDataToServer() {
            val delimiter = "@"
            val registerSocketMessage = "registerUserInfo$delimiter$coupleID$delimiter$userColumnID$delimiter$userName$delimiter$profileImageUrl"
            //TODO : 에뮬에서는 바로 실행이 되지만, 개인 기기에서는 lateinit property socketSenderThread has not been initialized 에러 발생 -> 해결책 찾자
            Handler().postDelayed({ chatService.sendMessage(registerSocketMessage) }, 300)
        }

        // 서버 클라이언트 소켓의 변수를 지정하기 위해 접속하자마자 데이터 전송
        private fun joinCoupleChat() {
            //TODO : 에뮬에서는 바로 실행이 되지만, 개인 기기에서는 lateinit property socketSenderThread has not been initialized 에러 발생 -> 해결책 찾자
            Handler().postDelayed({ chatService.sendMessage("joinCoupleChat") }, 300)
        }
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    /*
    바운드 서비스에서는 HTTPConnectionThread(HandlerThread)가 동작하고 있으며, 이 스레드에 메시지를 통해 서버에 요청을 보낸다
    서버에서 결과를 보내주면 HTTPConnectionThread(HandlerThread)의 인터페이스 메소드 -> 바운드 서비스 -> 바운드 서비스 인터페이스 -> 액티비티 onHttpRespond 에서 handle 한다
     */
    private val httpConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: HTTPConnectionService.LocalBinder = service as HTTPConnectionService.LocalBinder
            httpConnectionService = binder.getService()!!
            httpConnectionService.setOnHttpRespondListener(this@CoupleChattingActivity)

            httpConnectionService.serverGetRequest(makeRequestUrl("getCoupleChatHistory"), REQUEST_CHAT_HISTORY, GET_CHAT_HISTORY)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}