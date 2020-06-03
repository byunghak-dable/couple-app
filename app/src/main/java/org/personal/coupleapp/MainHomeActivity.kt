package org.personal.coupleapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_home.*
import org.personal.coupleapp.adapter.ItemClickListener
import org.personal.coupleapp.adapter.StoryAdapter
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.dialog.WarningDialog
import kotlin.collections.ArrayList

class MainHomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ItemClickListener,
    WarningDialog.DialogListener {

    // warning dialog 아이디 값
    private val FIRST_VISIT_DIALOG_ID = 1

    // 스토리 리스트(리사이클러 뷰) 관련 변수
    private val storyList by lazy { ArrayList<StoryData>() }
    private val storyAdapter by lazy { StoryAdapter(storyList, this) }

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

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        expandableAddBtn.setOnClickListener(this)
        calendarBtn.setOnClickListener(this)
        storyBtn.setOnClickListener(this)
    }


    private fun buildRecyclerView() {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)

        // 더미 데이터
        for (i in 1..5) {
            val slideModels = ArrayList<SlideModel>()
            val storyData: StoryData

            for (j in 1..5) {

                slideModels.add(SlideModel("https://byline.network/wp-content/uploads/2018/05/cat.png"))
            }
            storyData = StoryData(slideModels)
            storyList.add(storyData)
        }

        storyRV.setHasFixedSize(true)
        storyRV.layoutManager = layoutManager
        storyRV.adapter = storyAdapter
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    //TODO: 추후 데이터를 같이 보내야 한다.
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

    private fun expandMenuBtn() {

        if (isOpen) {

            handleAnimation(fabClose, fabClockwise, false)

        } else {

            handleAnimation(fabOpen, fabAntiClockwise, true)
            calendarBtn.isClickable
            storyBtn.isClickable
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
    }

    //TODO: 리사이클러 뷰 클릭 이벤트 구현
    override fun onItemClick(itemPosition: Int) {

    }

    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------
    // 롹인 버튼만 있는 warning 다이얼로그 이벤트 메소드
    override fun applyConfirm(id: Int) {
        when(id) {
            FIRST_VISIT_DIALOG_ID -> {
                val toProfileModify = Intent(this, ProfileModifyActivity::class.java)
                startActivity(toProfileModify)
            }
        }
    }
}
