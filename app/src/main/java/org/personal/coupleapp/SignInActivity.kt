package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

class SignInActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.name
    private lateinit var serverConnectionThread: ServerConnectionThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setListener()

//        val hashMap  = HashMap<String, Objects>()
//        val gSon = Gson()
//        val test = gSon.fromJson("{\"k1\":1,\"k2\":\"v2\"}",hashMap.javaClass )
//        Log.i(TAG, test["k1"].toString())
    }

    override fun onStart() {
        super.onStart()
        startWorkerThread()
    }

    override fun onStop() {
        super.onStop()
        stopWorkerThread()
    }

    // onCreate 보기 편하도록 클릭 리스너 모아두는 메소드
    private fun setListener() {
        signInBtn.setOnClickListener(this)
        googleSignInBtn.setOnClickListener(this)
        signUpTV.setOnClickListener(this)
        forgotTV.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        serverConnectionThread.looper.quit()
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
        val toHome = Intent(this, MainHomeActivity::class.java)
        startActivity(toHome)
    }

    // 회원가입으로 이동
    private fun signUp() {
        val toSignUp = Intent(this, SignUpFirstActivity::class.java)
        startActivity(toSignUp)
    }

    // TODO: 구현해야함
    private fun googleSignIn() {
        val intent = Intent(this, SignUpSecondActivity::class.java)
        startActivity(intent)
    }

    private fun test() {
        val handlerMessageHelper = HandlerMessageHelper
        handlerMessageHelper.serverPostRequest(serverConnectionThread, "SignIn", "{\"id\": \"sigletone\"}", 1, 1)
    }

    // TODO: 수정해야 함
    private class CustomHandler(activity: Activity) : Handler() {

        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
                    1 -> {

                        val toSignUp = Intent(activity, SignUpFirstActivity::class.java)
                        activity.startActivity(toSignUp)
                    }
                }
            } else {
                return
            }
        }
    }
}
