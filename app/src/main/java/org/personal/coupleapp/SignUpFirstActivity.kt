package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_sign_up_first.*

class SignUpFirstActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_first)

        setListener()
    }

    private fun setListener() {
        signUpBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.signUpBtn -> toSecondStep()
            R.id.googleSignUpBtn -> googleSignUp()
        }
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    // TODO: 데이터 이동 및 서버와의 통신 구현해야함

    // 회원가입 두번 째 단계로 가는 메소드
    private fun toSecondStep() {
        val toSecondStep = Intent(this, SignUpSecondActivity::class.java)
        startActivity(toSecondStep)
    }

    private fun googleSignUp() {

    }
}
