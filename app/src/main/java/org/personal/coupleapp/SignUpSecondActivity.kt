package org.personal.coupleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_sign_up_second.*

class SignUpSecondActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_second)

        setListener()
    }

    private fun setListener() {
        connectBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.connectBtn->connectWithOpponent()
        }
    }

    // TODO: 서버 연결을 통해 초대코드로 상대방과 연결하기
    private fun connectWithOpponent() {

    }
}
