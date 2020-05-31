package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_chat.*
import kotlinx.android.synthetic.main.activity_main_home.bottomNavigation

class MainChatActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_chat)
        setListener()
    }

    override fun onStart() {
        super.onStart()
        bottomNavigation.selectedItemId = R.id.chat
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        partnerTV.setOnClickListener(this)
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
        when(v?.id) {
            R.id.partnerTV -> toPartnerChatting()
        }
    }

    private fun toPartnerChatting() {
        val toPartnerChatting = Intent(this, ChattingActivity::class.java)
        startActivity(toPartnerChatting)
    }
}
