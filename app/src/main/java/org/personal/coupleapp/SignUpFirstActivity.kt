package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sign_up_first.*
import kotlinx.android.synthetic.main.activity_sign_up_first.emailED
import kotlinx.android.synthetic.main.activity_sign_up_first.passwordED
import org.json.JSONObject
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_SIMPLE_POST_METHOD
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class SignUpFirstActivity : AppCompatActivity(), View.OnClickListener, TextWatcher, HTTPConnectionListener {

    private val TAG = javaClass.name
    private val serverPage = "SignUp"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val loadingDialog by lazy { LoadingDialog() }

    // http 서버 통신 후 작업에 필요한 변수들
    private val CHECK_EMAIL_VALIDATION = 1
    private val TO_SECOND_STEP = 2

    // 이메일, 비밀번호 valid check 변수
    private var isEmailValid = false
    private var isPasswordValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_first)
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

    private fun setListener() {
        signUpBtn.setOnClickListener(this)
        emailED.addTextChangedListener(this)
        passwordED.addTextChangedListener(this)
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.signUpBtn -> toSecondStep()
            R.id.googleSignUpBtn -> googleSignUp()
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        when (s.hashCode()) {
            // 이메일 입력 시 validation check
            emailED.text.hashCode() -> {

                val isEmailFormatValid = android.util.Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()
                emailValidationTV.visibility = View.VISIBLE

                if (isEmailFormatValid) {
                    val postJsonObject = JSONObject()
                    postJsonObject.put("what", "emailValidation")
                    postJsonObject.put("email", s.toString())

                    httpConnectionService.serverPostRequest(serverPage, postJsonObject.toString(), REQUEST_SIMPLE_POST_METHOD, CHECK_EMAIL_VALIDATION)

                } else {
                    changeValidationStyle(emailValidationTV, R.string.emailInValid, R.color.red)
                }
            }

            // 비밀 번호 입력 시 validation check
            passwordED.text.hashCode() -> {

                val regex = Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\$@!%*#?&]).{8,15}.\$")
                PWValidationTV.visibility = View.VISIBLE

                isPasswordValid = if (s.toString().matches(regex)) {
                    changeValidationStyle(PWValidationTV, R.string.valid, R.color.green)
                    true
                } else {
                    changeValidationStyle(PWValidationTV, R.string.passwordValidation, R.color.red)
                    false
                }
            }
        }
    }

    // 사용자에게 이메일, 비밀번호 유효성 검사 결과를 알려주는 TextView 스타일 변경
    private fun changeValidationStyle(textView: TextView, text: Int, color: Int) {
        textView.text = getText(text)
        textView.setTextColor(this.getColor(color))
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    // 회원가입 두번 째 단계로 가는 메소드
    private fun toSecondStep() {

        if (isEmailValid) {
            if (isPasswordValid) {
                val email = emailED.text.toString()
                val password = passwordED.text.toString()
                val postJSONObject = JSONObject()
                postJSONObject.put("what", "signUpFirstStep")
                postJSONObject.put("email", email)
                postJSONObject.put("password", password)

                httpConnectionService.serverPostRequest(serverPage, postJSONObject.toString(), REQUEST_SIMPLE_POST_METHOD, TO_SECOND_STEP)
                // 로딩 다이얼로그 보여주기
                loadingDialog.show(supportFragmentManager, "Loading")
            } else {
                Toast.makeText(this, getText(R.string.checkPassword), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getText(R.string.checkEmail), Toast.LENGTH_SHORT).show()
        }
    }

    // TODO: 구글 회원가입 해야함
    private fun googleSignUp() {

    }

    // http 바인드 서비스 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {

            CHECK_EMAIL_VALIDATION -> {
                Log.i(TAG, "onResponseData : ${responseData["respondData"]}")

                when (responseData["respondData"] as String) {

                    "false" -> {
                        handler.post {
                            emailValidationTV?.text = getText(R.string.valid)
                            emailValidationTV?.setTextColor(getColor(R.color.green))
                        }
                        isEmailValid = true
                    }
                    else -> {
                        handler.post {
                            emailValidationTV?.text = getText(R.string.emailDuplicated)
                            emailValidationTV?.setTextColor(getColor(R.color.red))
                        }
                        isEmailValid = false
                    }
                }
            }
            TO_SECOND_STEP -> {
                loadingDialog.dismiss()
                val userTableID = parseInt(responseData["respondData"] as String)
                val toStepTwo = Intent(this, SignUpSecondActivity::class.java)
                val columnKey = getText(R.string.userColumnID).toString()
                val partnerConnection = getText(R.string.partnerConnection).toString()

                // sharedPreference 에 single_user(DB table)의 id 값, 파트너 연결 여부 저장
                SharedPreferenceHelper.setInt(this, columnKey, userTableID)
                SharedPreferenceHelper.setBoolean(this, partnerConnection, false)
                startActivity(toStepTwo)
                finish()
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
            httpConnectionService.setOnHttpRespondListener(this@SignUpFirstActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}
