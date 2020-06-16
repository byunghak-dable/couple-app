package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.json.JSONObject
import org.personal.coupleapp.AppRTCLibrary.ConnectActivity
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper

class SignInActivity : AppCompatActivity(), View.OnClickListener, HTTPConnectionListener {

    private val TAG = javaClass.name

    private val SIGN_IN = 1

    private val serverPage = "SignIn"
    private lateinit var httpConnectionService: HTTPConnectionService

    private val loadingDialog = LoadingDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setListener()
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
        signInBtn.setOnClickListener(this)
        googleSignInBtn.setOnClickListener(this)
        signUpTV.setOnClickListener(this)
        forgotTV.setOnClickListener(this)
    }

    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.signUpTV -> signUp()
            R.id.signInBtn -> signIn()
            R.id.googleSignInBtn -> googleSignIn()
            // TODO: test 지우자 후에
            R.id.forgotTV -> test()
        }
    }

    // 로그인 관리
    private fun signIn() {

        val intent = Intent(this, MainHomeActivity::class.java)
        startActivity(intent)
        // TODO: 테스트 주석 처리가 실제 코드
        val jsonObject = JSONObject()
//        jsonObject.put("what", "signIn")
//        jsonObject.put("email", emailED.text.toString())
//        jsonObject.put("password", passwordED.text.toString())
//
//        httpConnectionService.serverPostRequest(serverPage, jsonObject.toString(), REQUEST_SIGN_IN, SIGN_IN)
    }

    // 회원가입으로 이동
    private fun signUp() {
        val toSignUp = Intent(this, SignUpFirstActivity::class.java)
        startActivity(toSignUp)
    }

    // TODO: 구현해야함
    private fun googleSignIn() {
        val intent = Intent(this, ConnectActivity::class.java)
        startActivity(intent)
    }

    // TODO: test 용임 수정 필요
    private fun test() {
        val intent = Intent(this, SignUpSecondActivity::class.java)
        startActivity(intent)
    }

    // http 바인드 서비스 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {
            // 로그인을 할 경우
            SIGN_IN -> {
                when (responseData["respondData"]) {
                    // 만약
                    null -> {

                        handler.post { checkInputTV.visibility = View.VISIBLE }
                        handler.postDelayed({ checkInputTV.visibility = View.INVISIBLE }, 1000)
                    }
                    else -> {
                        val profileData: ProfileData = responseData["respondData"] as ProfileData
                        val toHome = Intent(this, MainHomeActivity::class.java)
                        Log.i(TAG, profileData.toString())
                        SharedPreferenceHelper.setInt(this, "userColumnID", profileData.id)
                        SharedPreferenceHelper.setInt(this, "coupleColumnID", profileData.couple_column_id!!)
                        SharedPreferenceHelper.setString(this, "userName", profileData.name)
                        SharedPreferenceHelper.setString(this, "profileImageUrl", profileData.profile_image as String)
                        SharedPreferenceHelper.setBoolean(this, "partnerConnection", true)

                        startActivity(toHome)
                        finish()
                    }
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
            httpConnectionService.setOnHttpRespondListener(this@SignInActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}