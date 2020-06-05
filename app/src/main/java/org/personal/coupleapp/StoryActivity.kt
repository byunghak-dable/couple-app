package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_story.*
import kotlinx.android.synthetic.main.activity_story.storyRV
import kotlinx.android.synthetic.main.activity_story.swipeRefreshSR
import org.json.JSONObject
import org.personal.coupleapp.StoryActivity.CustomHandler.Companion.GET_COUPLE_PROFILE_DATA
import org.personal.coupleapp.StoryActivity.CustomHandler.Companion.GET_STORY_DATA
import org.personal.coupleapp.adapter.ItemClickListener
import org.personal.coupleapp.adapter.StoryGridAdapter
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_COUPLE_PROFILE
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_SIMPLE_GET_METHOD
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_STORY_DATA
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import org.w3c.dom.Text
import java.lang.ref.WeakReference

class StoryActivity : AppCompatActivity(), View.OnClickListener, ItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private val TAG = javaClass.name

    private val serverPage = "Story"

    // 서버 통신 관련 스레드 생성
    private lateinit var serverConnectionThread: ServerConnectionThread

    private var isInitStoryData = false

    // 리사이클러 뷰 어뎁터 -> 스토리 데이터의 처음 사진만을 보여준다
    private val storyList = ArrayList<StoryData>()
    private val storyGridAdapter = StoryGridAdapter(this, storyList, this)
    private lateinit var scrollListener: InfiniteScrollListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)
        setListener()
        buildRecyclerView()
        startWorkerThread()
    }

    override fun onResume() {
        super.onResume()

        if (!isInitStoryData) {
            val profileRequestUrl = "$serverPage?coupleID"
            swipeRefreshSR.isRefreshing = true
            HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(0, "getCoupleProfile"), GET_COUPLE_PROFILE_DATA, REQUEST_COUPLE_PROFILE)
            HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(0, "getStoryData"), GET_STORY_DATA, REQUEST_STORY_DATA)
            isInitStoryData = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
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
                HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(page, "getStoryData"), GET_STORY_DATA, REQUEST_STORY_DATA)
            }
        }
        storyRV.addOnScrollListener(scrollListener)
        storyRV.adapter = storyGridAdapter
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this, senderProfileIV, senderNameTV,
            senderBirthdayTV, receiverProfileIV, receiverNameTV,receiverBirthdayTV, storyList, storyGridAdapter, swipeRefreshSR)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        serverConnectionThread.looper.quit()
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

    override fun onRefresh() {
        swipeRefreshSR.isRefreshing = true
        scrollListener.resetState()
        storyList.clear()
        storyGridAdapter.notifyDataSetChanged()
        HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(0, "getStoryData"), GET_STORY_DATA, REQUEST_STORY_DATA)
    }

    // 프로필 사진 업로드,
    private class CustomHandler(
        activity: Activity,
        senderProfileIV: ImageView,
        senderNameTV: TextView,
        senderBirthdayTV :TextView,
        receiverProfileIV: ImageView,
        receiverNameTV: TextView,
        receiverBirthdayTV:TextView,
        val storyList: ArrayList<StoryData>,
        val storyGridAdapter: StoryGridAdapter,
        swipeRefreshSR: SwipeRefreshLayout
    ) : Handler() {

        companion object {
            const val GET_STORY_DATA = 1
            const val GET_COUPLE_PROFILE_DATA = 2
        }

        private val TAG = javaClass.name

        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
        private val swipeRefreshSRWeak: WeakReference<SwipeRefreshLayout> = WeakReference(swipeRefreshSR)
        private val senderProfileIVWeak: WeakReference<ImageView> = WeakReference(senderProfileIV)
        private val senderNameTVWeak: WeakReference<TextView> = WeakReference(senderNameTV)
        private val senderBirthdayTVWeak: WeakReference<TextView> = WeakReference(senderBirthdayTV)
        private val receiverProfileIVWeak: WeakReference<ImageView> = WeakReference(receiverProfileIV)
        private val receiverNameTVWeak: WeakReference<TextView> = WeakReference(receiverNameTV)
        private val receiverBirthdayTVWeak: WeakReference<TextView> = WeakReference(receiverBirthdayTV)


        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
                    // 스토리 받는 메시지
                    GET_STORY_DATA -> {
                        swipeRefreshSRWeak.get()?.isRefreshing = false

                        if (msg.obj == null) {
                            Log.i(TAG, "데이터 더이상 없음")
                        } else {
                            val fetchedStoryList = msg.obj as ArrayList<StoryData>
                            fetchedStoryList.forEach { storyList.add(it) }

                            Log.i(TAG, storyList[0].id.toString())
                            storyGridAdapter.notifyDataSetChanged()
                        }
                    }

                    // 커플 프로필 데이터 받는 메시지
                    GET_COUPLE_PROFILE_DATA -> {
                        // 커플 데이터를 hashmap 으로 받는다
                        val coupleData = msg.obj as HashMap<*, *>
                        val senderData: ProfileData = coupleData["senderProfile"] as ProfileData
                        val receiverData: ProfileData = coupleData["receiverProfile"] as ProfileData

                        // sender 프로필 정보 입력
                        Glide.with(activity).load(senderData.profile_image).into(senderProfileIVWeak.get()!!)
                        senderNameTVWeak.get()?.text = senderData.name
                        senderBirthdayTVWeak.get()?.text = CalendarHelper.timeInMillsToDate(senderData.birthday)

                        // receiver 프로필 정보 입력
                        Glide.with(activity).load(receiverData.profile_image).into(receiverProfileIVWeak.get()!!)
                        receiverNameTVWeak.get()?.text = receiverData.name
                        receiverBirthdayTVWeak.get()?.text = CalendarHelper.timeInMillsToDate(receiverData.birthday)
                    }
                }
            } else {
                return
            }
        }
    }
}
