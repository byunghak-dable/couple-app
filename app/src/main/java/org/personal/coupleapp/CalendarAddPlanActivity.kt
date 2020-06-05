package org.personal.coupleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_calendar_add_plan.*
import org.personal.coupleapp.CalendarAddPlanActivity.CustomHandler.Companion.PLAN_DATA_CHANGED
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_INSERT_PLAN_DATA
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_UPDATE_PLAN_DATA
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.dialog.*
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*

class CalendarAddPlanActivity : AppCompatActivity(), View.OnClickListener, PlanTypeDialog.DialogListener, TimePickerDialog.TimePickerListener,
    InformDialog.DialogListener, DatePickerDialog.DatePickerListener, RadioButtonDialog.DialogListener {

    private val TAG = javaClass.name

    private val serverPage = "CalendarAddPlan"

    private lateinit var serverConnectionThread: ServerConnectionThread
    private val loadingDialog by lazy { LoadingDialog() }

    private val startCalendar by lazy { intent.getSerializableExtra("startCalendar") as Calendar }
    private val endCalendar by lazy { intent.getSerializableExtra("endCalendar") as Calendar }

    private var planDataID: Int? = null
    private var isPublic = true
    private var startTime: Long? = null
    private var endTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_add_plan)
        startWorkerThread()
        setListener()
        setInitData()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
    }

    override fun onBackPressed() {
        val warningDialog = InformDialog()
        val arguments = Bundle()

        arguments.putString("title", getText(R.string.goBackTitle).toString())
        arguments.putString("message", getText(R.string.goBackMessage).toString())
        arguments.putBoolean("needAction", true)

        warningDialog.arguments = arguments
        warningDialog.show(supportFragmentManager, "BackWarning")
    }

    private fun setInitData() {
        startDateBtn.text = DateFormat.getDateInstance().format(startCalendar.time)
        endDateBtn.text = DateFormat.getDateInstance().format(endCalendar.time)
        startTimeBtn.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(startCalendar.time)
        endTimeBtn.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(endCalendar.time)
    }

    private fun setListener() {
        confirmBtn.setOnClickListener(this)
        publicTV.setOnClickListener(this)
        privateTV.setOnClickListener(this)
        chooseTypeBtn.setOnClickListener(this)
        locationBtn.setOnClickListener(this)
        startDateBtn.setOnClickListener(this)
        startTimeBtn.setOnClickListener(this)
        endDateBtn.setOnClickListener(this)
        endTimeBtn.setOnClickListener(this)
        repeatBtn.setOnClickListener(this)
        notificationBtn.setOnClickListener(this)
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this, loadingDialog)

        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
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
            R.id.confirmBtn -> addPlanToServer()
            R.id.publicTV -> applyPlanClassification(R.color.green, R.color.black, R.color.mainBackground, true)
            R.id.privateTV -> applyPlanClassification(R.color.black, R.color.red, R.color.red, false)
            R.id.chooseTypeBtn -> chooseType()
            R.id.locationBtn -> chooseLocation()
            R.id.startDateBtn -> chooseStartDate()
            R.id.startTimeBtn -> chooseStartTime()
            R.id.endDateBtn -> chooseEndDate()
            R.id.endTimeBtn -> chooseEndTime()
            R.id.repeatBtn -> chooseRepeat()
            R.id.notificationBtn -> chooseAlarm()
        }
    }

    private fun addPlanToServer() {
        val title = titleED.text.toString()
        val dateType = chooseTypeBtn.text.toString()
        val memo = memoED.text.toString()
        val location = locationBtn.text.toString()
        val repeat = repeatBtn.text.toString()
        val notification = notificationBtn.text.toString()

        startTime = startCalendar.timeInMillis
        endTime = endCalendar.timeInMillis

        if (title.isNotEmpty()) {
            if (dateType != "유형 선택") {

                val coupleColumnID = SharedPreferenceHelper.getInt(this, getText(R.string.coupleColumnID).toString())
                // 일정 데이터 객체 생성
                val planData = PlanData(planDataID, coupleColumnID, isPublic, title, memo, dateType, location, startTime!!, endTime!!, repeat, notification)

                loadingDialog.show(supportFragmentManager, "LoadingDialog")
                if (planDataID == null) {
                    HandlerMessageHelper.serverPostRequest(serverConnectionThread, serverPage, planData, PLAN_DATA_CHANGED, REQUEST_INSERT_PLAN_DATA)
                } else {
                    HandlerMessageHelper.serverPutRequest(serverConnectionThread, serverPage, planData, PLAN_DATA_CHANGED, REQUEST_UPDATE_PLAN_DATA)

                }
            } else {
                Toast.makeText(this, "데이트 유형을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    // 공동 / 개인 선택 시 변화 적용 -> 상대방과 일정 공유할지 나만의 일정을 추가할 지 결정
    private fun applyPlanClassification(publicColor: Int, privateColor: Int, imageColor: Int, publicCheck: Boolean) {
        publicTV.setTextColor(getColor(publicColor))
        privateTV.setTextColor(getColor(privateColor))
        classificationIV.backgroundTintList = getColorStateList(imageColor)
        overlapClassificationIV.backgroundTintList = getColorStateList(imageColor)
        isPublic = publicCheck
    }

    // 유형 선택 하기
    private fun chooseType() {
        val planTypeDialog = PlanTypeDialog()
        planTypeDialog.show(supportFragmentManager, "PlanTypeDialog")
    }

    // TODO: 구글 맵 연동 후 주소 검색 기능 추가하기
    private fun chooseLocation() {

    }

    // 시작 날짜 선택하기
    private fun chooseStartDate() {
        val datePickerDialog = DatePickerDialog()
        val arguments = Bundle()

        arguments.putString("whichPicker", "startDate")

        datePickerDialog.arguments = arguments
        datePickerDialog.show(supportFragmentManager, "startDateDialog")
    }

    // 시작 시간 선택하기
    private fun chooseStartTime() {
        val timePickerDialog = TimePickerDialog()
        val arguments = Bundle()

        arguments.putString("whichTimePicker", "startTime")

        timePickerDialog.arguments = arguments
        timePickerDialog.show(supportFragmentManager, "StartTimeDialog")
    }

    // 종료 날짜 선택하기
    private fun chooseEndDate() {
        val datePickerDialog = DatePickerDialog()
        val arguments = Bundle()

        arguments.putString("whichPicker", "endDate")

        datePickerDialog.arguments = arguments
        datePickerDialog.show(supportFragmentManager, "EndDateDialog")
    }

    // 종료 시간 선택하기
    private fun chooseEndTime() {
        val timePickerDialog = TimePickerDialog()
        val arguments = Bundle()

        arguments.putString("whichTimePicker", "endTime")

        timePickerDialog.arguments = arguments
        timePickerDialog.show(supportFragmentManager, "EndTimeDialog")
    }

    // 반복 설정 선택하기
    private fun chooseRepeat() {
        val repeatPlanDialog = RadioButtonDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.repeat)

        repeatPlanDialog.arguments = arguments
        repeatPlanDialog.show(supportFragmentManager, "RepeatChoiceDialog")
    }

    // 알림 설정 선택하기
    private fun chooseAlarm() {
        val repeatPlanDialog = RadioButtonDialog()
        val arguments = Bundle()

        arguments.putInt("arrayResource", R.array.notification)

        repeatPlanDialog.arguments = arguments
        repeatPlanDialog.show(supportFragmentManager, "RepeatChoiceDialog")
    }

    //------------------ 다이얼로그 fragment 인터페이스 메소드 모음 ------------------
    override fun onPlanTypeChoice(planType: String, imageSource: Int) {
        chooseTypeBtn.text = planType
        planTypeIV.setImageResource(imageSource)
    }

    // 날짜(시작, 종료) 선택 시
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int, whichPicker: String?) {
        when (whichPicker) {
            "startDate" -> applyDateChoice(startDateBtn, year, month, dayOfMonth, startCalendar)
            "endDate" -> applyDateChoice(endDateBtn, year, month, dayOfMonth, endCalendar)
        }
    }

    // 시간(시작, 종료) 선택 시
    override fun onTimeSet(timePicker: TimePicker?, hour: Int, minute: Int, whichPicker: String?) {
        when (whichPicker) {
            "startTime" -> applyTimeChoice(startTimeBtn, hour, minute, startCalendar)
            "endTime" -> applyTimeChoice(endTimeBtn, hour, minute, endCalendar)
        }
    }

    // 반복, 알림 선택 시
    override fun onRadioBtnChoice(whichDialog: Int, choice: String) {
        when (whichDialog) {
            R.array.repeat -> repeatBtn.text = choice
            R.array.notification -> notificationBtn.text = choice
        }
    }

    // TODO: date 포맷을다시 year, date, dayOfMonth 로 변환 가능한지 찾는 중
    private fun applyDateChoice(dateBtn: Button, year: Int, month: Int, dayOfMonth: Int, calendar: Calendar) {
        dateBtn.text = CalendarHelper.setDateFormat(year, month, dayOfMonth)
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
    }

    // TODO: time 포맷을다시 hour, minute 으로 변환 가능한지 찾는 중
    private fun applyTimeChoice(timeBtn: Button, hour: Int, minute: Int, calendar: Calendar) {
        // 시스템에서 설정된 시간 표현법으로 시간을 출력한다
        timeBtn.text = CalendarHelper.setTimeFormat(hour, minute)
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
    }

    // alert 다이얼로그 확인 버튼 누를 때
    override fun applyConfirm() {
        finish()
    }

    private class CustomHandler(activity: Activity, loadingDialog: LoadingDialog) : Handler() {

        companion object {
            const val PLAN_DATA_CHANGED = 1
        }

        private val TAG = javaClass.name

        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
        private val loadingDialogWeak: WeakReference<LoadingDialog> = WeakReference(loadingDialog)


        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
                    PLAN_DATA_CHANGED -> {
                        loadingDialogWeak.get()?.dismiss()
                        Log.i(TAG, "통신 테스트${msg.obj}")
                        activity.finish()
                    }
                }
            } else {
                return
            }
        }
    }
}
