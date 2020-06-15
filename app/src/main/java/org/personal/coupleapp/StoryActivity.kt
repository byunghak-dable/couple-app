package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_story.*
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.adapter.StoryGridAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_COUPLE_PROFILE
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_STORY_DATA
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class StoryActivity : AppCompatActivity(), View.OnClickListener, ItemClickListener, SwipeRefreshLayout.OnRefreshListener,
    HTTPConnectionListener {

    private val TAG = javaClass.name

    private val serverPage = "Story"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val GET_STORY_DATA = 1
    private val GET_COUPLE_PROFILE_DATA = 2

    private val loadingDialog by lazy { LoadingDialog() }

    // 리사이클러 뷰 어뎁터 -> 스토리 데이터의 처음 사진만을 보여준다
    private val storyList = ArrayList<StoryData>()
    private val storyGridAdapter = StoryGridAdapter(this, storyList, this)
    private lateinit var scrollListener: InfiniteScrollListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)
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

    // onCreate 보기 편하도록 클릭 리스너 모아두는 메소드
    private fun setListener() {
        addStoryBtn.setOnClickListener(this)
        swipeRefreshSR.setOnRefreshListener(this)
    }

    // TODO : 어뎁터 만들어야 함
    private fun buildRecyclerView() {
        val gridLayoutManager = GridLayoutManager(this, 3)
        storyRV.setHasFixedSize(true)
        storyRV.layoutManager = gridLayoutManager
        scrollListener = object : InfiniteScrollListener(gridLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                httpConnectionService.serverGetRequest(makeRequestUrl(page, "getStoryData"), REQUEST_STORY_DATA, GET_STORY_DATA)
            }
        }
        storyRV.addOnScrollListener(scrollListener)
        storyRV.adapter = storyGridAdapter
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    // 무한 스크롤링 아이템 범위를 정해서 서버에 보낼 request url build 하는 메소드
    private fun makeRequestUrl(page: Int, whatRequest: String): String {
        val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())
        var requestUrl: String = "$serverPage?"
        when (whatRequest) {
            "getStoryData" -> requestUrl += "what=$whatRequest&&coupleID=$coupleColumnID&&page=$page"
            "getCoupleProfile" -> requestUrl += "what=$whatRequest&&coupleID=$coupleColumnID"
        }
        Log.i(TAG, "url check : $requestUrl")
        return requestUrl
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addStoryBtn -> toAddStory()
        }
    }

    private fun toAddStory() {
        val toAddStory = Intent(this, StoryAddActivity::class.java)
        startActivity(toAddStory)
    }

    //------------------ 인터페이스 이벤트 관리하는 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
        Log.i(TAG, storyList[itemPosition].id.toString())
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
    }

    // 리사이클러 뷰 새로 고침을 하게되면 스토리 데이터를 새로 가져온다
    override fun onRefresh() {
        swipeRefreshSR.isRefreshing = true
        scrollListener.resetState()
        storyList.clear()
        httpConnectionService.serverGetRequest(makeRequestUrl(0, "getStoryData"), REQUEST_STORY_DATA, GET_STORY_DATA)
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {
            // 로그인을 할 경우
            GET_STORY_DATA -> {
                swipeRefreshSR.isRefreshing = false

                when (responseData["respondData"]) {
                    null -> {
                        Log.i(TAG, "onHttpRespond : 데이터 더이상 없음")
                    }
                    else -> {
                        val fetchedStoryList = responseData["respondData"] as ArrayList<StoryData>
                        fetchedStoryList.forEach { storyList.add(it) }
                        handler.post { storyGridAdapter.notifyDataSetChanged() }
                        Log.i(TAG, "onHttpRespond : 스토리 데이터 받아옴")
                    }
                }
            }
            GET_COUPLE_PROFILE_DATA -> {
                loadingDialog.dismiss()
                // 커플 데이터를 hashmap 으로 받는다
                val coupleData = responseData["respondData"] as HashMap<*, *>
                val senderData: ProfileData = coupleData["senderProfile"] as ProfileData
                val receiverData: ProfileData = coupleData["receiverProfile"] as ProfileData

                // 커플 프로필 UI 작업
                handler.post {
                    // sender 프로필 정보 입력
                    Glide.with(this).load(senderData.profile_image).into(senderProfileIV)
                    senderNameTV.text = senderData.name
                    senderBirthdayTV.text = CalendarHelper.timeInMillsToDate(senderData.birthday)

                    // receiver 프로필 정보 입력
                    Glide.with(this).load(receiverData.profile_image).into(receiverProfileIV)
                    receiverNameTV.text = receiverData.name
                    receiverBirthdayTV.text = CalendarHelper.timeInMillsToDate(receiverData.birthday)
                }
            }
        }
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    /*
    바운드 서비스에서는 HTTPConnectionThread(HandlerThread)가 동작하고 있으며, 이 스레드에 메시지를 통해 서버에 요청을 보낸다
    서버에서 결과를 보내주면 HTTPConnectionThread(HandlerThread)의 인터페이스 메소드 -> 바운드 서비스 -> 바운드 서비스 인터페이스 -> 액티비티 onHttpRespond 에서 handle 한다
     */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: HTTPConnectionService.LocalBinder = service as HTTPConnectionService.LocalBinder
            httpConnectionService = binder.getService()!!
            httpConnectionService.setOnHttpRespondListener(this@StoryActivity)
            getInitData()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }

        // 초기 커플 프로필 데이터와 스토리 데이터 가져오기
        fun getInitData() {
            swipeRefreshSR.isRefreshing = true
            loadingDialog.show(supportFragmentManager, "LoadingDialog")

            storyList.clear()

            httpConnectionService.serverGetRequest(makeRequestUrl(0, "getCoupleProfile"), REQUEST_COUPLE_PROFILE, GET_COUPLE_PROFILE_DATA)
            httpConnectionService.serverGetRequest(makeRequestUrl(0, "getStoryData"), REQUEST_STORY_DATA, GET_STORY_DATA)
        }
    }
}
