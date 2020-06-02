package org.personal.coupleapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_modify_profile.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_profile.profileImageIV
import org.personal.coupleapp.ProfileActivity.CustomHandler.Companion.SHOW_PROFILE_INFO
import org.personal.coupleapp.backgroundOperation.ImageDecodeHandler
import org.personal.coupleapp.backgroundOperation.ImageDecodeThread
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

    private val TAG = javaClass.name

    private val serverPage = "Profile"

    private lateinit var serverConnectionThread: ServerConnectionThread
    private lateinit var imageDecodeThread: ImageDecodeThread

    // 로딩 다이얼로그
    private val loadingDialog = LoadingDialog()

    // 프로필 이미지의 Bitmap 정보를 담고 있는 리스트
    private val imageList: ArrayList<Bitmap> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setListener()
        startWorkerThread()
    }

    override fun onResume() {
        super.onResume()
        val singleUserID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())
        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, singleUserID, SHOW_PROFILE_INFO, REQUEST_PROFILE_INFO)
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
        val decodeMainHandler = ImageDecodeHandler(profileImageIV, imageList)

        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
        imageDecodeThread = ImageDecodeThread("ImageDecodeThread", this, decodeMainHandler)
        serverConnectionThread.start()
        imageDecodeThread.start()
    }


    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
        imageDecodeThread.looper.quit()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.modifyProfileIV -> toModifyProfile()
        }
    }

    // 프로필 변경 액티비티로 이동(이동할 때 데이터도 같이 전송)
    private fun toModifyProfile() {
        val toModifyProfile = Intent(this, ProfileModifyActivity::class.java)
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
                        val profileData = msg.obj as ProfileData
                        loadingWeak.get()?.dismiss()
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
