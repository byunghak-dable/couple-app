package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_sign_up_second.*
import org.json.JSONObject
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_SIMPLE_POST_METHOD
import org.personal.coupleapp.dialog.InformDialog
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.service.HTTPConnectionInterface
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class SignUpSecondActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = javaClass.name
    private val serverPage = "ConnectPartner"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val loadingDialog by lazy { LoadingDialog() }

    private val GET_INVITE_CODE = 1
    private val CHECK_CONNECTE_COMPLETED = 2
    private val CHECK_CONNECTION = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("thread-test", "onCreate")
        setContentView(R.layout.activity_sign_up_second)
        setListener()
    }

    override fun onStart() {
        super.onStart()
        Log.i("thread-test", "onStart")
        getDeepLinkParameter()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    //------------------ 액티비티 생명주기 콜백 메소드에서 호출할 메소드 모음 ------------------
    private fun setListener() {
        shareBtn.setOnClickListener(this)
        connectBtn.setOnClickListener(this)
        checkConnectedBtn.setOnClickListener(this)
    }

    private fun getDeepLinkParameter() {
        val uri: Uri? = intent.data
        if (uri != null) {
            val parameters: List<String> = uri.pathSegments
            val opponentCode = parameters[parameters.size - 1]
            opponentCodeED.setText(opponentCode)
        }
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.shareBtn -> shareInviteCode()
            R.id.connectBtn -> connectWithOpponent()
            R.id.checkConnectedBtn -> checkConnection()
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
        val invitationSenderID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())

        jsonObject.put("what", "connectToPartner")
        jsonObject.put("invitationReceiverID", invitationSenderID)
        jsonObject.put("invitationSenderCode", parseInt(opponentCodeED.text.toString()))

        httpConnectionService.serverPostRequest(serverPage, jsonObject.toString(), REQUEST_SIMPLE_POST_METHOD, CHECK_CONNECTE_COMPLETED)
        Log.i(TAG, "커플 DB에 업로드")
    }

    // 상대방과 연결을 확인하는 메소드 (자신의 singleUser table id 값을 보내어 coupleUser table 에 id 가 존재하는지 확인한다
    private fun checkConnection() {
        val jsonObject = JSONObject()
        val myColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())

        jsonObject.put("what", "checkConnection")
        jsonObject.put("id", myColumnID)

        httpConnectionService.serverPostRequest(serverPage, jsonObject.toString(), REQUEST_SIMPLE_POST_METHOD, CHECK_CONNECTION)

        Log.i(TAG, "Request : 상대방과의 연결확인하기")
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    /*
    바운드 서비스에서는 HTTPConnectionThread(HandlerThread)가 동작하고 있으며, 이 스레드에 메시지를 통해 서버에 요청을 보낸다
    서버에서 결과를 보내주면 HTTPConnectionThread(HandlerThread)의 인터페이스 메소드 -> 바운드 서비스 -> 바운드 서비스 인터페이스 -> 액티비티 onHttpRespond 에서 handle 한다
     */
    private val connection: ServiceConnection = object : ServiceConnection, HTTPConnectionInterface {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: HTTPConnectionService.LocalBinder = service as HTTPConnectionService.LocalBinder
            httpConnectionService = binder.getService()!!
            httpConnectionService.setOnHttpRespondListener(this)
            getInviteCode()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }

        override fun onHttpRespond(responseData: HashMap<*, *>) {
            // 메인 UI 작업을 담당하는 핸들러 -> 액티비티가 아닌 ServiceConnection 의 추상 클래스 내부에서 UI 를 다루기 위해 생성
            val handler = Handler(Looper.getMainLooper())

            when (responseData["whichRespond"] as Int) {

                GET_INVITE_CODE -> {
                    val invitationCode = responseData["respondData"].toString()
                    handler.post { myInviteCodeTV.text = invitationCode }
                    Log.i(TAG, "onResponseData : ${responseData["respondData"]}")
                }
                CHECK_CONNECTE_COMPLETED -> {
                    loadingDialog.dismiss()

                    when (responseData["respondData"].toString()) {
                        "false" -> {
                            failedDialog("상대방 코드를 확인해주세요")
                        }
                        else -> {
                            val coupleColumnID = parseInt(responseData["respondData"].toString())
                            onSignUpSuccess(this@SignUpSecondActivity, coupleColumnID)
                        }
                    }
                }
                CHECK_CONNECTION -> {
                    when (responseData["respondData"].toString()) {
                        "false" -> {
                            failedDialog("상대방과 연결이 되지 않았습니다")
                        }
                        else -> {
                            val coupleColumnID = parseInt(responseData["respondData"].toString())
                            onSignUpSuccess(this@SignUpSecondActivity, coupleColumnID)
                        }
                    }
                }

            }
        }

        // 회원가입 2단계에 접속하면 1단계 회원가입을 한 아이디의 초대코드를 가져오는 메소드
        private fun getInviteCode() {
            // singleUserColumn id 를 preference 에서 추출해 서버로 전송
            val postJsonObject = JSONObject()
            val userColumnID = SharedPreferenceHelper.getInt(this@SignUpSecondActivity, getText(R.string.userColumnID).toString())
            postJsonObject.put("what", "getInvitationCode")
            postJsonObject.put("singleUserID", userColumnID)

            httpConnectionService.serverPostRequest(serverPage, postJsonObject.toString(), REQUEST_SIMPLE_POST_METHOD, GET_INVITE_CODE)
            Log.i(TAG, "Request : 초대코드 서버로부터 받아오는 요청 보냄")
        }

        // 회원가입이 모두 완료됬을 때 호출하는 메소드
        private fun onSignUpSuccess(activity: AppCompatActivity, coupleColumnID: Int) {
            val toHome = Intent(activity, MainHomeActivity::class.java)
            val coupleColumnKey = activity.getText(R.string.coupleColumnID).toString()
            val partnerConnection = activity.getText(R.string.partnerConnection).toString()

            // 커플 컬럼 아이디를 shared 에 저장
            SharedPreferenceHelper.setInt(activity, coupleColumnKey, coupleColumnID)
            SharedPreferenceHelper.setBoolean(activity, partnerConnection, true)

            // 처음 홈에 들어가게 되면 프로필 설정 액티비티로 전환하기 위한 boolean 값을 같이 넘겨준다
            toHome.putExtra("firstTime", true)
            activity.startActivity(toHome)
        }

        // 실패했을 때 다이얼로그 보여주는 메소드
        private fun failedDialog(message: String) {
            val alertDialog = InformDialog()
            val arguments = Bundle()

            arguments.putString("title", "알림창")
            arguments.putString("message", message)

            alertDialog.arguments = arguments
            alertDialog.show(this@SignUpSecondActivity.supportFragmentManager, "FailedDialog")
        }
    }
}
