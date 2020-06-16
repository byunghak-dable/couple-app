package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_album.*
import kotlinx.android.synthetic.main.activity_main_album.bottomNavigation
import org.personal.coupleapp.adapter.AlbumAdapter
import org.personal.coupleapp.data.AlbumData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService

class MainAlbumActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ItemClickListener, HTTPConnectionListener {

    private val TAG = javaClass.name

    private lateinit var httpConnectionService: HTTPConnectionService


    private val albumList by lazy { ArrayList<AlbumData>() }
    private val albumAdapter by lazy { AlbumAdapter(this, albumList, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_album)
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

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.album
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        addFolderBtn.setOnClickListener(this)
    }

    private fun buildRecyclerView() {

        //TODO : 테스트
        for (i in 1..2) {
            val albumData = AlbumData(i, "쿠쿠", 3)
            albumList.add(albumData)
        }

        val layoutManager = GridLayoutManager(this, 2)

        albumFolderRV.setHasFixedSize(true)
        albumFolderRV.layoutManager = layoutManager
        albumFolderRV.adapter = albumAdapter
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    //------------------ 네비게이션 바 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> toHome()
            R.id.chat -> toChat()
            R.id.map -> toMap()
            R.id.profile -> toProfile()
        }
        overridePendingTransition(0, 0)
        return true
    }

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {

        }
    }

    //------------------ 리사이클러 뷰 아이템 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {

    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
    }

    // 네비게이션 바를 통해 이동하는 메소드
    private fun toHome() {
        val toHome = Intent(this, MainHomeActivity::class.java)
        startActivity(toHome)
    }

    private fun toChat() {
        val toChat = Intent(this, MainChatActivity::class.java)
        startActivity(toChat)
    }

    private fun toMap() {
        val toMap = Intent(this, MainMapActivity::class.java)
        startActivity(toMap)
    }

    private fun toProfile() {
        val toMore = Intent(this, MainProfileActivity::class.java)
        startActivity(toMore)
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        TODO("Not yet implemented")
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
            httpConnectionService.setOnHttpRespondListener(this@MainAlbumActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}
