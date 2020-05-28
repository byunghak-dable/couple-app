package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_home.*

class MainHomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    // floating 버튼 애니메이션 관련 변수
    private val fabOpen by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_open) }
    private val fabClose by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_close) }
    private val fabClockwise by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_clockwise) }
    private val fabAntiClockwise by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_anti_clockwise) }
    private var isOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_home)

        bottomNavigation.selectedItemId = R.id.home
        setListener()
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        expandableAddBtn.setOnClickListener(this)
        calendarBtn.setOnClickListener(this)
        addStoryBtn.setOnClickListener(this)
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    //TODO: 추후 데이터를 같이 보내야 한다.
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat -> toChat()
            R.id.album -> toAlbum()
            R.id.map -> toMap()
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

    private fun toMap() {
        val toMap = Intent(this, MainMapActivity::class.java)
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
            R.id.addStoryBtn -> toAddStory()
        }
    }

    private fun expandMenuBtn() {

        if (isOpen) {

            handleAnimation(fabClose, fabClockwise, false)

        } else {

            handleAnimation(fabOpen, fabAntiClockwise, true)
            calendarBtn.isClickable
            addStoryBtn.isClickable
        }
    }

    private fun toCalendar() {
        Toast.makeText(this, this.getText(R.string.checkEmail), Toast.LENGTH_SHORT).show()
    }

    private fun toAddStory() {
        Toast.makeText(this, this.getText(R.string.checkEmail), Toast.LENGTH_SHORT).show()
    }

    // floating 버튼 애니메이션을 핸들링 하는 메소드
    private fun handleAnimation(childAnimation : Animation, parentAnimation: Animation, isChileOpen: Boolean) {
        calendarBtn.startAnimation(childAnimation)
        calendarTV.startAnimation(childAnimation)
        addStoryBtn.startAnimation(childAnimation)
        addStoryTV.startAnimation(childAnimation)
        expandableAddBtn.startAnimation(parentAnimation)

        isOpen = isChileOpen
    }
}
