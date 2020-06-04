package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_home.*
import org.json.JSONObject
import org.personal.coupleapp.MainHomeActivity.CustomHandler.Companion.DELETE_STORY_DATA
import org.personal.coupleapp.MainHomeActivity.CustomHandler.Companion.GET_HOME_STORY_DATA
import org.personal.coupleapp.adapter.ItemClickListener
import org.personal.coupleapp.adapter.StoryAdapter
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.DELETE_METHOD
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_SIMPLE_GET_METHOD
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_STORY_DATA
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.dialog.ChoiceDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.dialog.WarningDialog
import org.personal.coupleapp.utils.InfiniteScrollListener
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt
import java.lang.ref.WeakReference

class MainHomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ItemClickListener,
    WarningDialog.DialogListener, SwipeRefreshLayout.OnRefreshListener, ChoiceDialog.DialogListener {

    private val TAG = javaClass.name

    // 홈 페이지라 domain 만 사용
    private val serverPage = ""

    private val MODIFY_STORY_DATA = 10

    // warning dialog 아이디 값
    private val FIRST_VISIT_DIALOG_ID = 1

    // 서버 통신관련 변수
    private lateinit var serverConnectionThread: ServerConnectionThread
    private val loadingDialog = LoadingDialog()

    // onResume 에서 초기 스토리 데이터 가져올지 여부 결정
    private var isInitStoryData = false

    // 스토리 리스트(리사이클러 뷰) 관련 변수
    private val storyList = ArrayList<StoryData>()
    private val storyAdapter = StoryAdapter(storyList, this)
    private lateinit var scrollListener: InfiniteScrollListener

    // floating 버튼 애니메이션 관련 변수
    private val fabOpen by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_open) }
    private val fabClose by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_close) }
    private val fabClockwise by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_clockwise) }
    private val fabAntiClockwise by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_anti_clockwise) }
    private var isOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_home)
        setListener()
        buildRecyclerView()
        startWorkerThread()

        // 처음 로그인했을 때 프로필 설정할 수 있도록 프로필 액티비티로 이동
        if (intent.getBooleanExtra("firstTime", false)) {
            val warningDialog = WarningDialog()
            val arguments = Bundle()

            arguments.putInt("dialogID", FIRST_VISIT_DIALOG_ID)
            arguments.putString("title", "환영합니다")
            arguments.putString("message", "프로필을 설정해주세요.\n확인을 누르면 프로필 설정창으로 이동합니다")

            warningDialog.arguments = arguments
            warningDialog.show(supportFragmentManager, "FirstTimeDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        bottomNavigation.selectedItemId = R.id.home
    }

    override fun onResume() {
        super.onResume()

        // 초기 스토리 데이터 불러왔는지 확인하고 불러오지 않았으면 스토리 데이터 요청
        if (!isInitStoryData) {
            swipeRefreshSR.isRefreshing = true
            HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(0), GET_HOME_STORY_DATA, REQUEST_STORY_DATA)
            isInitStoryData = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        expandableAddBtn.setOnClickListener(this)
        calendarBtn.setOnClickListener(this)
        storyBtn.setOnClickListener(this)
        swipeRefreshSR.setOnRefreshListener(this)
    }


    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        storyRV.setHasFixedSize(true)
        storyRV.layoutManager = layoutManager
        scrollListener = object : InfiniteScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(page), GET_HOME_STORY_DATA, REQUEST_STORY_DATA)
            }
        }
        storyRV.addOnScrollListener(scrollListener)
        storyRV.adapter = storyAdapter
    }


    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this, storyList, storyAdapter, swipeRefreshSR)

        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
    }

    // 무한 스크롤링 아이템 범위를 정해서 서버에 보낼 request url build 하는 메소드
    private fun makeRequestUrl(page: Int): String {
        val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())
        return "$serverPage?what=getStoryData&&coupleID=$coupleColumnID&&page=$page"
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat -> toChat()
            R.id.album -> toAlbum()
            R.id.notice -> toNotice()
            R.id.more -> toMore()
        }
        overridePendingTransition(0, 0)
        return true
    }

    private fun toChat() {
        val toChat = Intent(this, MainChatActivity::class.java)
        startActivity(toChat)
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
            R.id.expandableAddBtn -> expandMenuBtn()
            R.id.calendarBtn -> toCalendar()
            R.id.storyBtn -> toStory()
        }
    }

    // 만약 연필 모양의 플로팅 버튼을 추가한 경우 메소드 호출
    private fun expandMenuBtn() {

        if (isOpen) {

            handleAnimation(fabClose, fabClockwise, false)
        } else {

            handleAnimation(fabOpen, fabAntiClockwise, true)
        }
    }

    private fun toCalendar() {
        val toCalendar = Intent(this, CalendarActivity::class.java)
        startActivity(toCalendar)
    }

    private fun toStory() {
        val toStory = Intent(this, StoryActivity::class.java)
        startActivity(toStory)
    }

    // floating 버튼 애니메이션을 핸들링 하는 메소드
    private fun handleAnimation(childAnimation: Animation, parentAnimation: Animation, isChileOpen: Boolean) {
        calendarBtn.startAnimation(childAnimation)
        calendarTV.startAnimation(childAnimation)
        storyBtn.startAnimation(childAnimation)
        addStoryTV.startAnimation(childAnimation)
        expandableAddBtn.startAnimation(parentAnimation)

        isOpen = isChileOpen
        calendarBtn.isClickable = isChileOpen
        storyBtn.isClickable = isChileOpen
    }

    //------------------ 인터페이스 이벤트 관리하는 메소드 모음 ------------------
    // 새로 고침할 때 리스트 clear, 스크롤리스너 내부 변수 값 초기화, 새로운 데이터 불러오기 실행
    override fun onRefresh() {
        swipeRefreshSR.isRefreshing = true
        scrollListener.resetState()
        storyList.clear()
        storyAdapter.notifyDataSetChanged()
        HandlerMessageHelper.serverGetRequest(serverConnectionThread, makeRequestUrl(0), GET_HOME_STORY_DATA, REQUEST_STORY_DATA)
    }

    //TODO: 리사이클러 뷰 클릭 이벤트 구현
    override fun onItemClick(view: View?, itemPosition: Int) {
        val toolChoiceDialog = ChoiceDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.modifyOrDelete)
        arguments.putInt("itemPosition", itemPosition)
        arguments.putInt("id", storyList[itemPosition].id!!)


        toolChoiceDialog.arguments = arguments
        toolChoiceDialog.show(supportFragmentManager, "CameraOrGalleryDialog")
    }

    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------
    // 롹인 버튼만 있는 warning 다이얼로그 이벤트 메소드
    override fun applyConfirm(id: Int) {
        when (id) {
            FIRST_VISIT_DIALOG_ID -> {
                val toProfileModify = Intent(this, ProfileModifyActivity::class.java)
                startActivity(toProfileModify)
            }
        }
    }

    override fun onChoice(whichDialog: Int, choice: String, itemPosition: Int?, id: Int?) {
        when (choice) {
            "수정하기" -> modifyStory(itemPosition, id)
            "삭제하기" -> deleteStory(itemPosition, id)
        }
    }

    private fun modifyStory(itemPosition: Int?, storyID: Int?) {
        val toModifyStory = Intent(this, StoryAddActivity::class.java)
        toModifyStory.putExtra("storyData", storyList[itemPosition!!])
        startActivityForResult(toModifyStory, MODIFY_STORY_DATA)
    }

    private fun deleteStory(itemPosition: Int?, storyID: Int?) {
        val jsonObject = JSONObject()

        jsonObject.put("what", "deleteStory")
        jsonObject.put("itemPosition", itemPosition)
        jsonObject.put("storyID", storyID)

        HandlerMessageHelper.serverDeleteRequest(serverConnectionThread, serverPage, jsonObject.toString(), DELETE_STORY_DATA, DELETE_METHOD)
    }

    private class CustomHandler(
        activity: Activity,
        val storyList: ArrayList<StoryData>,
        val storyAdapter: StoryAdapter,
        swipeRefreshSR: SwipeRefreshLayout
    ) : Handler() {

        companion object {
            const val GET_HOME_STORY_DATA = 1
            const val DELETE_STORY_DATA = 2
        }

        private val TAG = javaClass.name

        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
        private val swipeRefreshSRWeak: WeakReference<SwipeRefreshLayout> = WeakReference(swipeRefreshSR)


        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
                    GET_HOME_STORY_DATA -> {
                        swipeRefreshSRWeak.get()?.isRefreshing = false
                        if (msg.obj == null) {
                            Log.i(TAG, "데이터 없음")
                        } else {
                            val fetchedStoryList = msg.obj as ArrayList<StoryData>
                            fetchedStoryList.forEach { storyList.add(it) }

                            Log.i(TAG, storyList[0].id.toString())
                            storyAdapter.notifyDataSetChanged()
                        }
                    }

                    DELETE_STORY_DATA -> {
                        storyList.removeAt(parseInt(msg.obj.toString()))
                        storyAdapter.notifyDataSetChanged()
                    }
                }
            } else {
                return
            }
        }
    }
}
