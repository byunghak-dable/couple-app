package org.personal.coupleapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sign_up_first.*
import org.json.JSONObject
import org.personal.coupleapp.SignUpFirstActivity.CustomHandler.Companion.CHECK_EMAIL_VALIDATION
import org.personal.coupleapp.SignUpFirstActivity.CustomHandler.Companion.TO_SECOND_STEP
import org.personal.coupleapp.SignUpFirstActivity.CustomHandler.Companion.isEmailValid
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_SIMPLE_POSTING
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt
import java.lang.ref.WeakReference

class SignUpFirstActivity : AppCompatActivity(), View.OnClickListener, TextWatcher {

    private val TAG = javaClass.name
    private val serverPage = "SignUp"
    private lateinit var serverConnectionThread: ServerConnectionThread

    // utils/singleton 싱글톤 객체
    // Memo : object 는 한번만 선언 가능
    private var isPasswordValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_first)
        setListener()
        // 스레드 시작
        startWorkerThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 스레드 종료
        stopWorkerThread()
    }

    private fun setListener() {
        signUpBtn.setOnClickListener(this)
        emailED.addTextChangedListener(this)
        passwordED.addTextChangedListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this, emailValidationTV)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        serverConnectionThread.looper.quit()
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

                    HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, postJsonObject.toString(), CHECK_EMAIL_VALIDATION, REQUEST_SIMPLE_POSTING)
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

                HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, postJSONObject.toString(), TO_SECOND_STEP, REQUEST_SIMPLE_POSTING)
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

    //------------------ ServerThread 와의 통신 결과를 안드로이드 Main UI 에 적용하는 Handler 클래스 ------------------
    private class CustomHandler(activity: AppCompatActivity, emailValidationTV: TextView) : Handler() {

        companion object {
            const val CHECK_EMAIL_VALIDATION = 1
            const val TO_SECOND_STEP = 2
            var isEmailValid = false
        }

        private val activityWeakReference: WeakReference<AppCompatActivity> = WeakReference(activity)
        private val textViewWeakReference: WeakReference<TextView> = WeakReference(emailValidationTV)
        private val preferenceHelper = SharedPreferenceHelper

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val emailValidationTV = textViewWeakReference.get()
            val activity = activityWeakReference.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    CHECK_EMAIL_VALIDATION -> {
                        val isDuplicated = msg.obj.toString()

                        if (isDuplicated == "false") {
                            emailValidationTV?.text = activity.getText(R.string.valid)
                            emailValidationTV?.setTextColor(activity.getColor(R.color.green))
                            isEmailValid = true
                        } else {
                            emailValidationTV?.text = activity.getText(R.string.emailDuplicated)
                            emailValidationTV?.setTextColor(activity.getColor(R.color.red))
                            isEmailValid = false
                        }
                    }

                    TO_SECOND_STEP -> {
                        // 큰 따옴표를 제거하여 값 추출
                        val userTableID = parseInt(msg.obj.toString())
                        val toStepTwo = Intent(activity, SignUpSecondActivity::class.java)
                        val columnKey = activity.getText(R.string.userColumnID).toString()
                        val partnerConnection = activity.getText(R.string.partnerConnection).toString()

                        // sharedPreference 에 single_user(DB table)의 id 값, 파트너 연결 여부 저장
                        preferenceHelper.setInt(activity, columnKey, userTableID)
                        preferenceHelper.setBoolean(activity, partnerConnection, false)
                        activity.startActivity(toStepTwo)
                        activity.finish()
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                // TODO: 로그 찍기
                return
            }
        }
    }
}
