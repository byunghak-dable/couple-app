package org.personal.coupleapp

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_profile.profileImageIV
import org.personal.coupleapp.ProfileActivity.CustomHandler.Companion.SHOW_PROFILE_INFO
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_PROFILE_INFO
import org.personal.coupleapp.data.ProfileData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.ImageEncodeHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.ref.WeakReference

class ProfileActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "ProfileActivity"

    private val serverPage = "Profile"

    private lateinit var serverConnectionThread: ServerConnectionThread

    // 로딩 다이얼로그
    private val loadingDialog = LoadingDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setListener()
        startWorkerThread()
        Log.e("ProfileActivity", "thread 종료")
    }

    override fun onResume() {
        super.onResume()
        val singleUserID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())
        val whatRequest = "getProfileData"
        val requestUrl = "$serverPage?what=$whatRequest&&id=$singleUserID"

        HandlerMessageHelper.serverGetRequest(serverConnectionThread, requestUrl, SHOW_PROFILE_INFO, REQUEST_PROFILE_INFO)
        loadingDialog.show(supportFragmentManager, "LoadingDialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
    }

    private fun setListener() {
        modifyProfileIV.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this, loadingDialog, profileImageIV, nameTV, stateTV, birthdayTV, sexTV)

        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
        serverConnectionThread.start()
    }


    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.modifyProfileIV -> toModifyProfile()
        }
    }

    // 프로필 변경 액티비티로 이동(이동할 때 데이터도 같이 전송)
    private fun toModifyProfile() {
        val toModifyProfile = Intent(this, ProfileModifyActivity::class.java)

        toModifyProfile.putExtra("name", nameTV.text.toString())
        toModifyProfile.putExtra("stateMessage", stateTV.text.toString())
        toModifyProfile.putExtra("birthday", birthdayTV.text)
        toModifyProfile.putExtra("sex", sexTV.text.toString())

        startActivity(toModifyProfile)
    }

    // 프로필 사진 업로드,
    private class CustomHandler(
        activity: AppCompatActivity,
        loadingDialog: LoadingDialog,
        profileIV: ImageView,
        nameTV: TextView,
        stateTV: TextView,
        birthDayTV: TextView,
        sexTV: TextView
    ) : Handler() {

        companion object {
            const val SHOW_PROFILE_INFO = 1
        }

        private val TAG = javaClass.name

        private val activityWeak: WeakReference<AppCompatActivity> = WeakReference(activity)
        private val loadingWeak: WeakReference<LoadingDialog> = WeakReference(loadingDialog)
        private val profileIVWeak: WeakReference<ImageView> = WeakReference(profileIV)
        private val nameTVWeak: WeakReference<TextView> = WeakReference(nameTV)
        private val stateTVWeak: WeakReference<TextView> = WeakReference(stateTV)
        private val birthDayTVWeak: WeakReference<TextView> = WeakReference(birthDayTV)
        private val sexTVWeak: WeakReference<TextView> = WeakReference(sexTV)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeak.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    SHOW_PROFILE_INFO -> {
                        Log.i("되나", "머지?")
                        val profileData = msg.obj as ProfileData
                        // 로딩 다이얼로그 삭제
                        loadingWeak.get()?.dismiss()
                        // 액티비티 view 값 지정

                        // 싱글턴에 비트맵을 저장한다 -> 추후 프로필 수정 액티비티에서 사용하도록
                        // TODO: 싱글턴 말고 intent 로 되지 않아 다음과 같은 방법을 사용... 방법 더 찾아보자
                        ImageEncodeHelper.bitmapList.clear()
                        CalendarHelper.timeList.clear()
                        ImageEncodeHelper.bitmapList.add(profileData.profile_image as Bitmap)
                        CalendarHelper.timeList.add(profileData.birthday)

                        profileIVWeak.get()?.setImageBitmap(profileData.profile_image as Bitmap)
                        nameTVWeak.get()?.text = profileData.name
                        stateTVWeak.get()?.text = profileData.state_message
                        birthDayTVWeak.get()?.text = CalendarHelper.timeInMillsToDate(profileData.birthday)
                        sexTVWeak.get()?.text = profileData.sex
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                Log.i(TAG, "액티비티 제거로 인해 handler 종료")
                return
            }
        }
    }
}
