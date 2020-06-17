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
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_album.*
import kotlinx.android.synthetic.main.activity_main_album.bottomNavigation
import org.json.JSONObject
import org.personal.coupleapp.adapter.AlbumFolderAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_GET_ALBUM_FOLDER
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_ALBUM_FOLDER_DATA
import org.personal.coupleapp.data.AlbumFolderData
import org.personal.coupleapp.dialog.AddAlbumDialog
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class MainAlbumActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ItemClickListener, HTTPConnectionListener,
    AddAlbumDialog.DialogListener, InformDialog.DialogListener {

    private val TAG = javaClass.name

    private val serverPage = "MainAlbum"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val ADD_ALBUM_FOLDER = 1
    private val GET_ALBUM_FOLDER = 2
    private val DELETE_FOLDER = 3

    private val loadingDialog by lazy { LoadingDialog() }

    private val albumList by lazy { ArrayList<AlbumFolderData>() }
    private val albumAdapter by lazy { AlbumFolderAdapter(this, albumList, this) }

    private val coupleID by lazy { SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString()) }

    // 아이템을 길게 클릭하면 삭제할지 여부를 물어보는 다이얼로그를 보여주는데 그 때 해당 아이템의 id를 저장하는 변수
    private var targetFolderID : Int? = null

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

    private fun makeRequestUrl(what: String): String {
        return "$serverPage?what=$what&&coupleID=$coupleID"
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

    //------------------ 버튼 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addFolderBtn -> addAlbumFolder()
        }
    }

    private fun addAlbumFolder() {
        val addAlbumDialog = AddAlbumDialog()
        addAlbumDialog.show(supportFragmentManager, "AddAlbumDialog")
    }

    //------------------ 리사이클러 뷰 아이템 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onItemClick(view: View?, itemPosition: Int) {
        val toAlbumGallery = Intent(this, AlbumGalleryActivity::class.java)
        toAlbumGallery.putExtra("folderID", albumList[itemPosition].id)
        startActivity(toAlbumGallery)
    }

    override fun onItemLongClick(view: View?, itemPosition: Int) {
        val deleteDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", "사진 삭제")
        arguments.putString("message", "선택하신 사진을 삭제하시겠습니까?")
        arguments.putBoolean("needAction", true)

        deleteDialog.arguments = arguments
        deleteDialog.show(supportFragmentManager, "BackWarning")
        targetFolderID = albumList[itemPosition].id
    }

    //------------------ 다이얼로그 이벤트 관리하는 메소드 모음 ------------------

    // 앨범 폴더를 추가하는 메소드 -> 서버에 폴더 추가를 요청한다.
    override fun onAddAlbumCompleted(folderName: String) {
        val albumFolderData = AlbumFolderData(null, coupleID, folderName, null, null)
        loadingDialog.show(supportFragmentManager, "LoadingDialog")
        httpConnectionService.serverPostRequest(serverPage, albumFolderData, REQUEST_INSERT_ALBUM_FOLDER_DATA, ADD_ALBUM_FOLDER)
    }

    override fun applyConfirm() {
        val deleteJsonObject = JSONObject()
        deleteJsonObject.put("what", "deleteFolder")
        deleteJsonObject.put("id", targetFolderID)
        Log.i(TAG, deleteJsonObject.toString())
        httpConnectionService.serverDeleteRequest(serverPage, deleteJsonObject.toString(), HTTPConnectionThread.DELETE_FROM_SERVER, DELETE_FOLDER)
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        loadingDialog.dismiss()
        when (responseData["whichRespond"] as Int) {
            ADD_ALBUM_FOLDER -> {
                Log.i(TAG, "http test : ${responseData["respondData"]}")
                if (responseData["respondData"] == 201) {
                    albumList.clear()
                    loadingDialog.show(supportFragmentManager, "LoadingDialog")
                    httpConnectionService.serverGetRequest(makeRequestUrl("getAlbumData"), REQUEST_GET_ALBUM_FOLDER, GET_ALBUM_FOLDER)
                } else {
                    Log.i(TAG, "서버 연결 실패")
                }
            }
            GET_ALBUM_FOLDER -> {
                if (responseData["respondData"] != null) {
                    val fetchedAlbumList =  responseData["respondData"] as ArrayList<AlbumFolderData>
                    fetchedAlbumList.forEach { albumList.add(it) }
                    handler.post { albumAdapter.notifyDataSetChanged() }
                } else{
                    handler.post { albumAdapter.notifyDataSetChanged() }
                }
            }
            DELETE_FOLDER-> {
                if (responseData["respondData"] != null) {
                    albumList.clear()
                    loadingDialog.show(supportFragmentManager, "LoadingDialog")
                    httpConnectionService.serverGetRequest(makeRequestUrl("getAlbumData"), REQUEST_GET_ALBUM_FOLDER, GET_ALBUM_FOLDER)
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
            httpConnectionService.setOnHttpRespondListener(this@MainAlbumActivity)

            albumList.clear()
            loadingDialog.show(supportFragmentManager, "LoadingDialog")
            httpConnectionService.serverGetRequest(makeRequestUrl("getAlbumData"), REQUEST_GET_ALBUM_FOLDER, GET_ALBUM_FOLDER)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}
