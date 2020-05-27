package org.personal.coupleapp

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_sign_up_second.*
import org.json.JSONObject
import org.personal.coupleapp.SignUpSecondActivity.CustomHandler.Companion.GET_INVITE_CODE
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_POSTING
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt
import java.lang.ref.WeakReference

class SignUpSecondActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.name
    private val serverPage = "ConnectPartner"
    private lateinit var serverConnectionThread: ServerConnectionThread

    // utils/singleton 싱글톤 객체
    private val handlerMessageHelper = HandlerMessageHelper
    private val preferenceHelper = SharedPreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_second)
        setListener()
    }

    override fun onStart() {
        super.onStart()
        // 스레드를 onStart 에서 시작하고 onStop 에서 종료
        startWorkerThread()
    }

    override fun onStop() {
        super.onStop()
        // 스레드 종료
        stopWorkerThread()
    }

    override fun onResume() {
        super.onResume()

        val postJsonObject = JSONObject()
        val userColumnID = preferenceHelper.getInt(this, this.getText(R.string.userColumnID).toString())
        postJsonObject.put("what", "getInvitationCode")
        postJsonObject.put("singleUserID", userColumnID)
        handlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, postJsonObject.toString(), GET_INVITE_CODE, REQUEST_POSTING)
    }

    private fun setListener() {
        shareBtn.setOnClickListener(this)
        connectBtn.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this, myInviteCodeTV)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        serverConnectionThread.looper.quit()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.shareBtn -> shareInviteCode()
            R.id.connectBtn -> connectWithOpponent()
        }
    }

    // TODO: 상대방에게 초대코드 공유하기
    private fun shareInviteCode() {

    }

    // TODO: 서버 연결을 통해 초대코드로 상대방과 연결하기
    private fun connectWithOpponent() {

    }

    private class CustomHandler(activity: Activity, myInviteCode: TextView) : Handler() {

        companion object {
            const val GET_INVITE_CODE = 1
        }

        private val activityWeak: WeakReference<Activity> = WeakReference(activity)
        private val myInviteCodeWeak:WeakReference<TextView> = WeakReference(myInviteCode)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeak.get()
            val myInviteCode = myInviteCodeWeak.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    GET_INVITE_CODE -> {
                        val invitationCode = msg.obj.toString()
                        myInviteCode?.text = invitationCode
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                return
            }
        }
    }
}
