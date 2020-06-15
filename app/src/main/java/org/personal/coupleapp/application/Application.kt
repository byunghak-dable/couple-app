package org.personal.coupleapp.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class Application : Application() {

    companion object {
        // 채널 ID
        const val CHAT_NOTIFICATION_CHANNEL_ID = "Chatting"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // Application 이 실행되면 바로 채널을 생성하도록 하기 위해 App클래스에서 채널 생성 메소드 구현
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // 채널에 대한 기본 정보 현재는 하나의 채널만 사용하고 있다. -> 포그라운드 서비스 사용을 위해 두개로 늘림
            // 푸시 알림으로 사용자에게 알림이 온 것을 알려주기 위해 중요도를 높음으로 설정
            val importanceHigh = NotificationManager.IMPORTANCE_HIGH
            val importanceLow = NotificationManager.IMPORTANCE_LOW

            // 약물 복용 시 알림
            val medicineChannel = NotificationChannel(CHAT_NOTIFICATION_CHANNEL_ID, "ChatNotification", importanceHigh)
            medicineChannel.description = "This channel is for chat message notification"

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(medicineChannel)
        }
    }
}
