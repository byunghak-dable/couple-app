package org.personal.coupleapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import org.personal.coupleapp.SignUpSecondActivity.CustomHandler.Companion.SIGN_UP_COMPLETED
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_POSTING
import org.personal.coupleapp.dialog.CustomAlertDialog
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt
import java.lang.ref.WeakReference

class SignUpSecondActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.name
    private val serverPage = "ConnectPartner"
    private lateinit var serverConnectionThread: ServerConnectionThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("thread-test", "onCreate")
        setContentView(R.layout.activity_sign_up_second)
        setListener()
        startWorkerThread()
    }

    override fun onStart() {
        super.onStart()
        Log.i("thread-test", "onStart")
        getDeepLinkParameter()
    }

    override fun onResume() {
        super.onResume()
        Log.i("thread-test", "onResume")
        getInviteCode()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("thread-test", "onDestroy")
        stopWorkerThread()
    }

    //------------------ 액티비티 생명주기 콜백 메소드에서 호출할 메소드 모음 ------------------
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
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
    }

    private fun getInviteCode() {
        // singleUserColumn id 를 preference 에서 추출해 서버로 전송
        val postJsonObject = JSONObject()
        val userColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())

        postJsonObject.put("what", "getInvitationCode")
        postJsonObject.put("singleUserID", userColumnID)

        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, postJsonObject.toString(), GET_INVITE_CODE, REQUEST_POSTING)
        Log.i(TAG, "초대코드 서버로부터 받아오는 요청 보냄")
        Log.i("thread-test", "onResume 끝")
    }

    private fun getDeepLinkParameter() {
        val uri: Uri? = intent.data
        if (uri != null) {
            val parameters: List<String> = uri.pathSegments
            val opponentCode = parameters[parameters.size - 1]
            opponentCodeED.setText(opponentCode)
        }
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.shareBtn -> shareInviteCode()
            R.id.connectBtn -> connectWithOpponent()
        }
    }

    // 상대방에게 초대코드 공유하기
    private fun shareInviteCode() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        val invitationCode = myInviteCodeTV.text.toString()
        sharingIntent.type = "text/html"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, getText(R.string.serverLink).toString() + "DeepLink/" + invitationCode)
        startActivity(Intent.createChooser(sharingIntent, "sharing"))
    }

    // 서버 연결을 통해 초대코드로 상대방과 연결하기
    private fun connectWithOpponent() {
        val jsonObject = JSONObject()
        val invitationSenderID =  SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())

        jsonObject.put("what", "connectToPartner")
        jsonObject.put("invitationSenderID", invitationSenderID)
        jsonObject.put("invitationReceiverCode", parseInt(opponentCodeED.text.toString()))

        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, jsonObject.toString(), SIGN_UP_COMPLETED, REQUEST_POSTING)
        Log.i(TAG, "커플 DB에 업로드")
    }

    // 초대 코드를 받아오고,
    private class CustomHandler(activity: AppCompatActivity, myInviteCode: TextView) : Handler() {

        companion object {
            const val GET_INVITE_CODE = 1
            const val SIGN_UP_COMPLETED = 2
        }

        private val activityWeak: WeakReference<AppCompatActivity> = WeakReference(activity)
        private val myInviteCodeWeak: WeakReference<TextView> = WeakReference(myInviteCode)

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

                    //TODO: 회원 가입 2단계 완료
                    SIGN_UP_COMPLETED -> {
                        when (msg.obj.toString()) {
                            "true" -> {
                                val toHome = Intent(activity, MainHomeActivity::class.java)

//                                SharedPreferenceHelper.setInt(activity, activity.getText(R.string.coupleColumnID).toString(), )

                            }

                            "false" -> {
                                val alertDialog = CustomAlertDialog()
                                val arguments = Bundle()

                                arguments.putString("title", "알림창")
                                arguments.putString("message", "상대방 코드를 확인해주세요")

                                alertDialog.arguments = arguments
                                alertDialog.show(activity.supportFragmentManager, "WrongOpponentCode")
                            }
                        }
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                return
            }
        }
    }
}

// 소켓 사용 해보기
//        Thread(Runnable {
//
//            try {
//                Log.i(TAG, "thread 시작")
//                val message = opponentCodeED.text.toString()
//                Log.i(TAG, message)
//                val socket = Socket("13.125.99.215", 20205)
//                Log.i(TAG, "소켓 연결")
//                val writer = PrintWriter(socket.getOutputStream())
//                writer.write(message)
//                writer.flush()
//                writer.close()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.i(TAG, "error")
//            }
//
//        }).start()