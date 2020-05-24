package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity(), View.OnClickListener  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        setListener()
    }

    // onCreate 보기 편하도록 클릭 리스너 모아두는 메소드
    private fun setListener() {
        signInBtn.setOnClickListener(this)
        signUpTV.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.signUpTV -> signUp()
            R.id.signInBtn -> signIn()
        }
    }

    // 회원가입으로 이동
    private fun signUp() {
        val toSignUp = Intent(this, SignUpFirstStepActivity::class.java)
        startActivity(toSignUp)
    }

    // 로그인 관리 -> 서버와의  필요
    private fun signIn() {
        val toHome = Intent(this, MainHomeActivity::class.java)
        startActivity(toHome)
    }
}
