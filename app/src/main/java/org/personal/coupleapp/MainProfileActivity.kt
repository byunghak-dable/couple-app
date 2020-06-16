package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main_profile.*
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_PROFILE_INFO
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import kotlin.collections.HashMap

class MainProfileActivity : AppCompatActivity(), View.OnClickListener, HTTPConnectionListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private val TAG = "ProfileActivity"

    private val serverPage = "Profile"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val SHOW_PROFILE_INFO = 1

    // 로딩 다이얼로그
    private val loadingDialog = LoadingDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_profile)
        setListener()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.profile
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun setListener() {
        bottomNavigation.setOnNavigationItemSelectedListener(this)
        modifyProfileIV.setOnClickListener(this)
        logOutBtn.setOnClickListener(this)
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
            R.id.album -> toAlbum()
            R.id.map -> toMap()
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

    private fun toAlbum() {
        val toAlbum = Intent(this, MainAlbumActivity::class.java)
        startActivity(toAlbum)
    }

    private fun toMap() {
        val toMap = Intent(this, MainMapActivity::class.java)
        startActivity(toMap)
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.modifyProfileIV -> toModifyProfile()
            R.id.logOutBtn -> logOut()
        }
    }

    private fun logOut() {
        val toSignIn = Intent(this, SignInActivity::class.java)
        startActivity(toSignIn)
    }

    // 프로필 변경 액티비티로 이동(이동할 때 데이터도 같이 전송)
    private fun toModifyProfile() {
        val toModifyProfile = Intent(this, ProfileModifyActivity::class.java)

        toModifyProfile.putExtra("name", nameTV.text.toString())
        toModifyProfile.putExtra("stateMessage", stateTV.text.toString())
        toModifyProfile.putExtra("birthday", birthdayTV.text)
        toModifyProfile.putExtra("sex", sexTV.text.toString())

        startActivity(toModifyProfile)
    }

    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {
            // 스토리를 불러오는 경우
            SHOW_PROFILE_INFO -> {
                loadingDialog.dismiss()
                val profileData = responseData["respondData"] as ProfileData

                // 싱글턴에 비트맵을 저장한다 -> 추후 프로필 수정 액티비티에서 사용하도록
                // TODO: 싱글턴 말고 intent 로 되지 않아 다음과 같은 방법을 사용... 방법 더 찾아보자
                ImageEncodeHelper.bitmapList.clear()
                CalendarHelper.timeList.clear()
                ImageEncodeHelper.bitmapList.add(profileData.profile_image as Bitmap)
                CalendarHelper.timeList.add(profileData.birthday)

                // 메인 UI 변경
                handler.post {
                    profileImageIV.setImageBitmap(profileData.profile_image as Bitmap)
                    nameTV.text = profileData.name
                    stateTV.text = profileData.state_message
                    birthdayTV.text = CalendarHelper.timeInMillsToDate(profileData.birthday)
                    sexTV.text = profileData.sex
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
            httpConnectionService.setOnHttpRespondListener(this@MainProfileActivity)
            getProfileData()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }

        // 프로필 데이터를 보여주는 메소드
        private fun getProfileData() {
            val singleUserID = SharedPreferenceHelper.getInt(this@MainProfileActivity, getText(R.string.userColumnID).toString())
            val whatRequest = "getProfileData"
            val requestUrl = "$serverPage?what=$whatRequest&&id=$singleUserID"

            loadingDialog.show(supportFragmentManager, "LoadingDialog")
            httpConnectionService.serverGetRequest(requestUrl, REQUEST_PROFILE_INFO, SHOW_PROFILE_INFO)
        }
    }
}
