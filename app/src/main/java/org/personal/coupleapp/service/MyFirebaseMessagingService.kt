package org.personal.coupleapp.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.personal.coupleapp.CoupleChattingActivity
import org.personal.coupleapp.R
import org.personal.coupleapp.application.Application.Companion.CHAT_NOTIFICATION_CHANNEL_ID
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.Integer.parseInt

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = javaClass.name

    private val GROUP_KEY = "org.personal.coupleapp.message"

    companion object {
        const val ACTION_SEND_COUPLE_CHAT = "INTENT_ACTION_SEND_COUPLE_CHAT"
        const val ACTION_SEND_OPEN_CHAT = "INTENT_ACTION_SEND_OPEN_CHAT"
    }

    /* 새로운 토큰을 파이어베이스로부터 받으면 shared preference 에 저장 -> 서버에 userColumnID 와 함께 저장하기 위해 대기
    TODO: 다음 과정 하기
    1. 회원 가입 완료 시 -> 서버 데이터 베이스에 userColumnID 와 함께 저장
    2. 로그인 시 -> 서버 데이터 베이스에 userColumnID 수정
     */
    override fun onNewToken(token: String) {

        Log.i(TAG, "onNewToken : $token")
        // 새로운 토큰을 받으면 shared preference 에 저장해 놓고, 회원가입이 완료 되거나 로그인을 했을 시에 DB 에 업데이트 해준다.
        SharedPreferenceHelper.setString(this, getText(R.string.firebaseMessagingToken).toString(), token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 사용자가 해당 메시지의 채팅방에 들어와 있는지를 확인하는 변수 -> ChattingActivity 에서 handling 한다.
        val currentRoomID = SharedPreferenceHelper.getInt(this, getText(R.string.joinedChatRoomNumber).toString())

        if (!remoteMessage.data["roomID"].isNullOrEmpty()) {
            val roomID = parseInt(remoteMessage.data["roomID"]!!)

            if (roomID != currentRoomID) {
                Glide.with(this).asBitmap().load(remoteMessage.data["profileImageUrl"]).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        showNotification(remoteMessage.data["name"], remoteMessage.data["message"], resource, roomID)
                        passOpenChatMessage(remoteMessage.data["whichChat"], roomID, remoteMessage.data["message"])
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            }
        }
    }

    private fun passOpenChatMessage(whichChat: String?, roomID: Int, message: String?) {
        val toChat = Intent()

        when (whichChat) {
            "couple" -> {
                toChat.action = ACTION_SEND_COUPLE_CHAT
                toChat.putExtra("message", message)
                Log.i(TAG, "passOpenChatMessage $whichChat")
            }

            "open" -> {
                toChat.action = ACTION_SEND_OPEN_CHAT
                toChat.putExtra("roomID", roomID)
                toChat.putExtra("message", message)
                Log.i(TAG, "passOpenChatMessage $whichChat")
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(toChat)
    }

    // 메시지 알림 notification 만드는 메소드
    private fun showNotification(senderName: String?, message: String?, profileImageBitmap: Bitmap?, notificationID:Int) {
        val intent = Intent(this, CoupleChattingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHAT_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(true)
            .setContentTitle(senderName)
            .setContentText(message)
            .setSmallIcon(R.drawable.couple_logo)
            .setLargeIcon(profileImageBitmap)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY)
            .build()

        val manager = NotificationManagerCompat.from(this)
        manager.notify(notificationID, notification)
    }
}

//         현 디바이스의 토큰 값을 확인할 수 있는 코드 -> 테스트 필요할 때 가져다 쓰기
//        FirebaseInstanceId.getInstance().instanceId
//            .addOnCompleteListener(OnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.w(TAG, "getInstanceId failed", task.exception)
//                    return@OnCompleteListener
//                }
//
//                // Get new Instance ID token
//                val token = task.result?.token
//
//                // Log and toast
//                Log.i(TAG, token)
//                SharedPreferenceHelper.setString(this, getText(R.string.firebaseMessagingToken).toString(), token)
//            })
