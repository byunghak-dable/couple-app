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
import kotlinx.android.synthetic.main.activity_chatting.*
import org.personal.coupleapp.adapter.ChatAdapter
import org.personal.coupleapp.data.CoupleChatData
import org.personal.coupleapp.service.ChatService
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class ChattingActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.name

    private lateinit var chatService: ChatService

    private val coupleChatList by lazy { ArrayList<CoupleChatData>() }
    private val chatAdapter: ChatAdapter by lazy { ChatAdapter(this, userColumnID, coupleChatList) }

    private val coupleID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString()) }
    private val userColumnID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString()) }
    private val userName by lazy { SharedPreferenceHelper.getString(this, getText(R.string.userName).toString()) }
    private val profileImageUrl by lazy { SharedPreferenceHelper.getString(this, getText(R.string.profileImageUrl).toString()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatting)
        setListener()
        buildRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
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
        val startService = Intent(this, ChatService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
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
        val messageData = "$coupleID$delimiter$userColumnID$delimiter$userName$delimiter$profileImageUrl$delimiter$message"
        val values = messageData.split(delimiter)

        chatService.sendMessage(messageData)
        chattingInputED.text = null

        Log.i(TAG, messageData)
        Log.i(TAG, values.toString())
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    private val connection: ServiceConnection = object : ServiceConnection, ChatService.ChatRespondListener {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: ChatService.LocalBinder = service as ChatService.LocalBinder
            chatService = binder.getService()!!
            chatService.setOnChatRespondListener(this)
            sendInitDataToServer()
            Log.i(TAG, "Bound Service Connection : Connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "Bound Service Connection : 바운드 서비스 종료")
        }

        // 서비스에 있는 리스너를 통해 메시지를 읽어 온다.
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
            val coupleChatData = CoupleChatData(coupleColumnID, userColumnID, senderName, profileImageUrl, message, messageTime)

            coupleChatList.add(coupleChatData)

            handler.post {
                chatAdapter.notifyItemInserted(coupleChatList.indexOf(coupleChatData))
                chattingBoxRV.scrollToPosition(chatAdapter.itemCount - 1)
                Log.i(TAG, coupleChatList.toString())
            }
        }

        // 서버 클라이언트 소켓의 변수를 지정하기 위해 접속하자마자 데이터 전송
        private fun sendInitDataToServer() {
            val delimiter = "@"
            val registerSocketMessage = "registerSocket$delimiter$coupleID$delimiter$userColumnID"
            chatService.sendMessage(registerSocketMessage)
        }
    }
}