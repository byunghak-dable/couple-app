package org.personal.coupleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.DatePicker
import kotlinx.android.synthetic.main.activity_modify_profile.*
import org.json.JSONObject
import org.personal.coupleapp.ProfileModifyActivity.CustomHandler.Companion.UPLOAD_PROFILE
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_POSTING
import org.personal.coupleapp.dialog.CustomInformDialog
import org.personal.coupleapp.dialog.DatePickerDialog
import org.personal.coupleapp.dialog.RadioButtonDialog
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.ref.WeakReference

class ProfileModifyActivity : AppCompatActivity(), View.OnClickListener, DatePickerDialog.DatePickerListener, RadioButtonDialog.DialogListener,
    CustomInformDialog.DialogListener {

    private val TAG = javaClass.name
    private val serverPage = "ModifyProfile"
    private lateinit var serverConnectionThread : ServerConnectionThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_profile)
        setListener()
        startWorkerThread()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("thread-test", "onDestroy")
        stopWorkerThread()
    }

    override fun onBackPressed() {
        val warningDialog = CustomInformDialog()
        val arguments = Bundle()

        arguments.putString("title", getText(R.string.goBackTitle).toString())
        arguments.putString("message", getText(R.string.goBackMessage).toString())
        arguments.putBoolean("needAction", true)

        warningDialog.arguments = arguments
        warningDialog.show(supportFragmentManager, "BackWarning")
    }

    private fun setListener() {
        profileImageIV.setOnClickListener(this)
        birthdayBtn.setOnClickListener(this)
        sexBtn.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val mainHandler = CustomHandler(this)
        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", mainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
    }

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.profileImageIV -> chooseProfileImage()
            R.id.birthdayBtn -> chooseBirthday()
            R.id.sexBtn -> chooseSex()
            R.id.confirmBtn -> confirmChange()
        }
    }

    private fun chooseProfileImage() {
//        val
    }


    private fun chooseBirthday() {
        val datePickerDialog = DatePickerDialog()
        datePickerDialog.show(supportFragmentManager, "startDateDialog")
    }

    private fun chooseSex() {
        val repeatPlanDialog = RadioButtonDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.sex)

        repeatPlanDialog.arguments = arguments
        repeatPlanDialog.show(supportFragmentManager, "RepeatChoiceDialog")
    }

    private fun confirmChange() {
        val postJsonObject = JSONObject()
        val userColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.userColumnID).toString())

        postJsonObject.put("id", userColumnID)
        postJsonObject.put("name", nameTV.text.toString())
        postJsonObject.put("state", stateTV.text.toString())
        postJsonObject.put("birthday", birthdayBtn.text.toString())
        postJsonObject.put("sex", sexBtn.text.toString())

        HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, postJsonObject.toString(), UPLOAD_PROFILE, REQUEST_POSTING)
        Log.i(TAG, "프로필 변경 서버에 업로드 메시지 보냄")
    }

    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int, whichPicker: String?) {
        birthdayBtn.text = CalendarHelper.setDateFormat(year, month, dayOfMonth)
    }

    override fun onRadioBtnChoice(whichDialog: Int, choice: String) {
        sexBtn.text = choice
    }

    override fun applyConfirm() {
        finish()
    }

    // 초대 코드를 받아오고,
    private class CustomHandler(activity: AppCompatActivity) : Handler() {

        companion object {
            const val UPLOAD_PROFILE = 1
        }

        private val activityWeak: WeakReference<AppCompatActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeak.get()

            // 액티비티가 destroy 되지 않았을 때
            if (activity != null) {
                when (msg.what) {
                    UPLOAD_PROFILE -> {
                        Log.i("되나?", msg.obj.toString())
                    }
                }
                // 액티비티가 destroy 되면 바로 빠져나오도록
            } else {
                return
            }
        }
    }
}
