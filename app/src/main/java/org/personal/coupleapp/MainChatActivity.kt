package org.personal.coupleapp

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_chat.*
import kotlinx.android.synthetic.main.activity_main_chat.bottomNavigation
import kotlinx.android.synthetic.main.activity_main_chat.swipeRefreshSR
import org.json.JSONObject
import org.personal.coupleapp.adapter.OpenChatRoomAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.DELETE_FROM_SERVER
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_COUPLE_PROFILE
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_OPEN_CHAT_ROOM_LIST
import org.personal.coupleapp.data.OpenChatRoomData
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.service.MyFirebaseMessagingService.Companion.ACTION_SEND_COUPLE_CHAT
import org.personal.coupleapp.service.MyFirebaseMessagingService.Companion.ACTION_SEND_OPEN_CHAT
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class MainChatActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ItemClickListener, HTTPConnectionListener,
    InformDialog.DialogListener {

    private val TAG = javaClass.name

    private val serverPage = "MainChat"

    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var httpConnectionService: HTTPConnectionService
    private val GET_COUPLE_PROFILE = 1
    private val GET_OPEN_CHAT_LIST = 2
    private val EXIT_OPEN_CHAT_ROOM = 3

    private val openChatRoomList by lazy { ArrayList<OpenChatRoomData>() }
    private val openChatRoomAdapter by lazy { OpenChatRoomAdapter(this, openChatRoomList, this) }
    private lateinit var scrollListener: InfiniteScrollListener
    private var selectedRoomID: Int? = null

    private val userColumnID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_chat)
        setListener()
        buildRecyclerView()
        defineReceiver()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_SEND_COUPLE_CHAT)
            addAction(ACTION_SEND_OPEN_CHAT)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
        bottomNavigation.selectedItemId = R.id.chat
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onStop() {
        super.onStop()
        unbindService(httpConnection)
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        partnerTV.setOnClickListener(this)
        addFriendsCoupleIB.setOnClickListener(this)
    }

    // 캘린더 일정 리사이클러 뷰 빌드
    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        openChatListRV.setHasFixedSize(true)
        openChatListRV.layoutManager = layoutManager

        scrollListener = object : InfiniteScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                httpConnectionService.serverGetRequest(makeRequestUrl("getOpenChatList", page), REQUEST_OPEN_CHAT_ROOM_LIST, GET_OPEN_CHAT_LIST)
            }
        }
        openChatListRV.addOnScrollListener(scrollListener)
        openChatListRV.adapter = openChatRoomAdapter
    }

    private fun defineReceiver() {

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i(TAG, "working?")
                when (intent?.action) {
                    ACTION_SEND_COUPLE_CHAT -> {
                        val message = intent.getStringExtra("message")
                        messageTV.text = message
                        Log.i(TAG, "defineReceiver couple chat : $message")
                    }

                    ACTION_SEND_OPEN_CHAT -> {

                    }
                }
            }
        }
    }

    // 바운드 서비스와 연결하는 메소드
    private fun startBoundService() {
        val startHttpService = Intent(this, HTTPConnectionService::class.java)
        bindService(startHttpService, httpConnection, BIND_AUTO_CREATE)
    }

    // 무한 스크롤링 아이템 범위를 정해서 서버에 보낼 request url build 하는 메소드
    private fun makeRequestUrl(whatRequest: String, page: Int? = null): String {
        val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())
        var requestUrl = "$serverPage?"
        when (whatRequest) {
            "getCoupleProfile" -> requestUrl += "what=$whatRequest&&coupleID=$coupleColumnID"
            "getOpenChatList" -> requestUrl += "what=$whatRequest&&userColumnID=$userColumnID&&page=$page"
        }
        Log.i(TAG, "url check : $requestUrl")
        return requestUrl
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> toHome()
            R.id.album -> toAlbum()
            R.id.notice -> toNotice()
            R.id.more -> toMore()
        }
        overridePendingTransition(0, 0)
        return true
    }

    // 네비게이션 바를 통해 이동하는 메소드
    //TODO: 추후 데이터를 같이 보내야 한다.
    private fun toHome() {
        val toHome = Intent(this, MainHomeActivity::class.java)
        startActivity(toHome)
    }

    private fun toAlbum() {
        val toAlbum = Intent(this, MainAlbumActivity::class.java)
        startActivity(toAlbum)
    }

    private fun toNotice() {
        val toMap = Intent(this, MainNoticeActivity::class.java)
        startActivity(toMap)
    }

    private fun toMore() {
        val toMore = Intent(this, MainMoreActivity::class.java)
        startActivity(toMore)
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.partnerTV -> toPartnerChatting()
            R.id.addFriendsCoupleIB -> toOpenChat()
        }
    }

    private fun toPartnerChatting() {
        val toCoupleChat = Intent(this, CoupleChattingActivity::class.java)
        startActivity(toCoupleChat)
    }

    private fun toOpenChat() {
        val toOpenChat = Intent(this, OpenChatActivity::class.java)
        startActivity(toOpenChat)
    }


    //------------------ 라사이클러 뷰 아이템 클릭 리스너 메소드 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
        val toOpenChat = Intent(this, OpenChattingActivity::class.java).apply {
            putExtra("roomID", openChatRoomList[itemPosition].id)
        }
        startActivity(toOpenChat)
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
        val exitDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", "오픈 채팅방 나가기")
        arguments.putString("message", "선택하신 오픈 채팅방을 나가시겠습니까?")
        arguments.putBoolean("needAction", true)

        exitDialog.arguments = arguments
        exitDialog.show(supportFragmentManager, "ExitDialog")
        selectedRoomID = openChatRoomList[itemPosition].id
    }

    override fun applyConfirm() {

        val jsonObject = JSONObject()
        jsonObject.put("what", "exitOpenChatRoom")
        jsonObject.put("roomID", selectedRoomID)
        jsonObject.put("userColumnID", userColumnID)
        httpConnectionService.serverDeleteRequest(serverPage, jsonObject.toString(), DELETE_FROM_SERVER, EXIT_OPEN_CHAT_ROOM)
    }

    //------------------ http 통신 결과를 받는 인터페이스 메소드 ------------------
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {

            // 상대방의 채팅 프로필 데이터를 받아온다(두 커플의 데이터를 가져오고 상대방의 프로필을 사용)
            //TODO : 데이터 베이스에서 상대방 프로필만을 가져오도록 구현해야하지만, 싱글 유저 테이블에 컬럼 추가가 필요해서 일단은 다음과 같이 구현 -> 시간이 남으면 구현하기
            GET_COUPLE_PROFILE -> {
                val coupleData = responseData["respondData"] as HashMap<*, *>
                val senderData: ProfileData = coupleData["senderProfile"] as ProfileData
                val receiverData: ProfileData = coupleData["receiverProfile"] as ProfileData

                when (userColumnID) {
                    senderData.id -> {
                        handler.post {
                            partnerTV.text = receiverData.name
                            Glide.with(this).load(receiverData.profile_image).into(profileImageIV)
                        }
                    }

                    receiverData.id -> {
                        handler.post {
                            partnerTV.text = senderData.name
                            Glide.with(this).load(senderData.profile_image).into(profileImageIV)
                        }
                    }
                }
            }
            // 오픈 채팅방 리스트 서버에게 받아오는 경우
            GET_OPEN_CHAT_LIST -> {
                Log.i(TAG, "http test : ${responseData["respondData"]}")
                swipeRefreshSR.isRefreshing = false

                if (responseData["respondData"] == null) {
                    Log.i(TAG, "오픈 채팅방 불러오기 : 데이터 없음")
                    handler.post { openChatRoomAdapter.notifyDataSetChanged() }
                } else {
                    val fetchedOpenChatList = responseData["respondData"] as ArrayList<OpenChatRoomData>
                    fetchedOpenChatList.forEach { openChatRoomList.add(it) }
                    handler.post { openChatRoomAdapter.notifyDataSetChanged() }
                    Log.i(TAG, "오픈 채팅방 불러오기 : 불러오기 완료")
                }
            }

            EXIT_OPEN_CHAT_ROOM -> {
                Log.i(TAG, "http 테스트 : ${responseData["respondData"]}")
                when (responseData["respondData"]) {
                    "true"-> {
                        handler.post { swipeRefreshSR.isRefreshing = true }
                        scrollListener.resetState()
                        openChatRoomList.clear()
                        httpConnectionService.serverGetRequest(makeRequestUrl("getOpenChatList", 0), REQUEST_OPEN_CHAT_ROOM_LIST, GET_OPEN_CHAT_LIST)
                    }
                    else -> Log.i(TAG, "EXIT_OPEN_CHAT_ROOM : 문제 발생")
                }
            }
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
            httpConnectionService.setOnHttpRespondListener(this@MainChatActivity)

            // 초기 커플 프로필 정보, 오픈 채팅방 정보를 가져온다
            openChatRoomList.clear()
            httpConnectionService.serverGetRequest(makeRequestUrl("getCoupleProfile"), REQUEST_COUPLE_PROFILE, GET_COUPLE_PROFILE)
            httpConnectionService.serverGetRequest(makeRequestUrl("getOpenChatList", 0), REQUEST_OPEN_CHAT_ROOM_LIST, GET_OPEN_CHAT_LIST)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}
