package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_video_call_receiver.*
import org.personal.coupleapp.AppRTCLibrary.ConnectActivity

class VideoCallReceiverActivity : AppCompatActivity() {

    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call_receiver)

        // 영상통화 방 이름 정보가 있는지 확인
        if (intent.hasExtra("roomID")) {
            val roomID = intent.getStringExtra("roomID")
            val profileImageUrl = intent.getStringExtra("profileImageUrl")
            Glide.with(this).load(profileImageUrl).into(profileIV)

            // 통화 버튼을 누르면 방정보를 영상통화 액티비티로 보내어 영상통화 참여하도록 한다
            answerVideoCallIB.setOnClickListener {
                val toVideoCall = Intent(this, ConnectActivity::class.java).apply {
                    putExtra("roomID", roomID)
                }
                startActivity(toVideoCall)
                finish()
            }
            // 영상통화 방 이름 정보가 없으면 로그와 함께 액티비티 종료
        }else {
            Log.i(TAG, "인텐트에 방정보가 없음")
            finish()
        }
    }
}