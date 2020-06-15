package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_open_chat.*
import org.personal.coupleapp.adapter.OpenChatRoomAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_OPEN_CHAT_USER
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_OPEN_CHAT_ROOM_LIST
import org.personal.coupleapp.data.OpenChatUserData
import org.personal.coupleapp.data.OpenChatRoomData
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.dialog.WarningDialog
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class OpenChatActivity : AppCompatActivity(), HTTPConnectionListener, View.OnClickListener, ItemClickListener, InformDialog.DialogListener
,WarningDialog.DialogListener{

    private val TAG = javaClass.name
    private val serverPage = "OpenChat"
    private lateinit var httpConnectionService: HTTPConnectionService

    private val GET_OPEN_CHAT_ROOM_LIST = 1
    private val UPLOAD_OPEN_CHAT_USER = 2

    private val openChatList = ArrayList<OpenChatRoomData>()
    private val openChatRoomAdapter = OpenChatRoomAdapter(this, openChatList, this)
    private lateinit var scrollListener: InfiniteScrollListener

    private var openChatRoomID: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_chat)
        setListener()
        buildRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(httpConnection)
    }

    private fun setListener() {
        addOpenChatBtn.setOnClickListener(this)
    }

    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        openChatRV.setHasFixedSize(true)
        openChatRV.layoutManager = layoutManager
        scrollListener = object : InfiniteScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                httpConnectionService.serverGetRequest(makeRequestUrl(page), REQUEST_OPEN_CHAT_ROOM_LIST, GET_OPEN_CHAT_ROOM_LIST)
            }
        }
        openChatRV.addOnScrollListener(scrollListener)
        openChatRV.adapter = openChatRoomAdapter
    }

    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, httpConnection, BIND_AUTO_CREATE)
    }

    // 무한 스크롤링 아이템 범위를 정해서 서버에 보낼 request url build 하는 메소드
    private fun makeRequestUrl(page: Int): String {
        return "$serverPage?what=getOpenChatList&&page=$page"
    }

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addOpenChatBtn -> addOpenChatRoom()
        }
    }

    private fun addOpenChatRoom() {
        val toCreateOpenChat = Intent(this, OpenChatAddRoomActivity::class.java)
        startActivity(toCreateOpenChat)
    }

    //------------------ 리사이클러 뷰 이벤트 관리하는 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
        val informDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", "오픈 채팅방 참여")
        arguments.putString("message", "선택하신 채팅방에 참여하시겠습니까?")
        arguments.putBoolean("needAction", true)

        informDialog.arguments = arguments
        informDialog.show(supportFragmentManager, "BackWarning")
        openChatRoomID = openChatList[itemPosition].id
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
        TODO("Not yet implemented")
    }

    //------------------ 다이얼로그 이벤트 관리하는 메소드 모음 ------------------
    // inform dialog : 오픈 채팅방에 참여할 지 말지를 결정하는 다이얼로그 이벤트
    override fun applyConfirm() {
        val userColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())
        val userName = SharedPreferenceHelper.getString(this, getText(R.string.userName).toString())
        val profileImageUrl = SharedPreferenceHelper.getString(this, getText(R.string.profileImageUrl).toString())
        val openChatParticipantsData = OpenChatUserData(openChatRoomID, userColumnID, userName, profileImageUrl)
        httpConnectionService.serverPostRequest(serverPage, openChatParticipantsData, REQUEST_INSERT_OPEN_CHAT_USER, UPLOAD_OPEN_CHAT_USER)
    }

    // 경고 다이얼로그 : 이미 참여한 오픈 채팅방인 경우 경고 다이얼로그를 보여준다
    // 다이얼로그 dismiss 만 하기 때문에 별다른 조치는 취하지 않음
    override fun applyConfirm(id: Int?) {

    }


    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {

            GET_OPEN_CHAT_ROOM_LIST -> {
                swipeRefreshSR.isRefreshing = false

                if (responseData["respondData"] == null) {
                    Log.i(TAG, "오픈채팅방 불러오기 : 데이터 없음")
                } else {
                    val fetchedOpenChatList = responseData["respondData"] as ArrayList<OpenChatRoomData>
                    fetchedOpenChatList.forEach { openChatList.add(it) }
                    handler.post { openChatRoomAdapter.notifyDataSetChanged() }
                    Log.i(TAG, "오픈채팅방 불러오기 : 불러오기 완료")
                }
            }
            UPLOAD_OPEN_CHAT_USER -> {
                Log.i(TAG, "테스트 : ${responseData["respondData"]}")

                when (responseData["respondData"]) {

                    "false" -> {
                        val warningDialog = WarningDialog()
                        val arguments = Bundle()

                        arguments.putString("title", "알림창")
                        arguments.putString("message", "이미 참여한 오픈채팅방입니다.")

                        warningDialog.arguments = arguments
                        warningDialog.show(supportFragmentManager, "NotifyDialog")
                    }

                    else -> {
                        val toOpenChat = Intent(this, OpenChattingActivity::class.java).apply {
                            putExtra("roomID", openChatRoomID)
                        }
                        startActivity(toOpenChat)
                        finish()
                        Log.i(TAG, "다이얼로그 테스트 : $openChatRoomID")
                        Log.i(TAG, "오픈채팅방으로 이동 : 성공")
                    }
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
            httpConnectionService.setOnHttpRespondListener(this@OpenChatActivity)

            swipeRefreshSR.isRefreshing = true
            openChatList.clear()
            httpConnectionService.serverGetRequest(makeRequestUrl(0), REQUEST_OPEN_CHAT_ROOM_LIST, GET_OPEN_CHAT_ROOM_LIST)
            Log.i(TAG, "바운드 서비스 연결")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}