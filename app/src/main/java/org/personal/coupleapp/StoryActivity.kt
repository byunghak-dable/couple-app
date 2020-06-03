package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.activity_story.*
import kotlinx.android.synthetic.main.activity_story.storyRV
import kotlinx.android.synthetic.main.activity_story.swipeRefreshSR
import org.json.JSONObject
import org.personal.coupleapp.StoryActivity.CustomHandler.Companion.GET_STORY_DATA
import org.personal.coupleapp.adapter.ItemClickListener
import org.personal.coupleapp.adapter.StoryGridAdapter
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_SIMPLE_GET_METHOD
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
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
            swipeRefreshSR.isRefreshing = true
            HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, makeItemRangeInJson(0), GET_STORY_DATA, REQUEST_SIMPLE_GET_METHOD)
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
                HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, makeItemRangeInJson(page), GET_STORY_DATA, REQUEST_SIMPLE_GET_METHOD)
            }
        }
        storyRV.addOnScrollListener(scrollListener)
        storyRV.adapter = storyGridAdapter
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this, storyList, storyGridAdapter, swipeRefreshSR)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        serverConnectionThread.looper.quit()
    }

    // 무한 스크롤링 아이템 범위를 정해서 jsonString 으로 변환(서버에 보낼)하는 메소드
    private fun makeItemRangeInJson(page: Int): String {
        val jsonObject = JSONObject()
        val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())

        jsonObject.put("what", "getStoryData")
        jsonObject.put("coupleID", coupleColumnID)
        jsonObject.put("page", page)

        return jsonObject.toString()
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
        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, makeItemRangeInJson(0), GET_STORY_DATA, REQUEST_SIMPLE_GET_METHOD)
    }

    // 프로필 사진 업로드,
    private class CustomHandler(
        activity: Activity,
        val storyList: ArrayList<StoryData>,
        val storyGridAdapter: StoryGridAdapter,
        swipeRefreshSR: SwipeRefreshLayout
    ) : Handler() {

        companion object {
            const val GET_STORY_DATA = 1
            const val GET_PROFILE_DATA = 2
        }

        private val TAG = javaClass.name

        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
        private val swipeRefreshSRWeak: WeakReference<SwipeRefreshLayout> = WeakReference(swipeRefreshSR)


        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
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

                    GET_PROFILE_DATA -> {
                        Log.i(TAG, "")
                    }
                }
            } else {
                return
            }
        }
    }
}
