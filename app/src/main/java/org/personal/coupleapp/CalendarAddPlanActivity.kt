package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_calendar_add_plan.*
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_INSERT_PLAN_DATA
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_UPDATE_PLAN_DATA
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.dialog.*
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.singleton.CalendarHelper
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.text.DateFormat
import java.util.*
import kotlin.collections.HashMap

class CalendarAddPlanActivity : AppCompatActivity(), View.OnClickListener, PlanTypeDialog.DialogListener, TimePickerDialog.TimePickerListener,
    InformDialog.DialogListener, DatePickerDialog.DatePickerListener, RadioButtonDialog.DialogListener,
    HTTPConnectionListener {

    private val TAG = javaClass.name

    private val serverPage = "CalendarAddPlan"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val PLAN_DATA_CHANGED = 1

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
        setListener()
        setInitData()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
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

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
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
                    httpConnectionService.serverPostRequest(serverPage, planData, REQUEST_INSERT_PLAN_DATA, PLAN_DATA_CHANGED)
                } else {
                    httpConnectionService.serverPutRequest(serverPage, planData, REQUEST_UPDATE_PLAN_DATA, PLAN_DATA_CHANGED)


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

    // http 바인드 서비스 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        when (responseData["whichRespond"] as Int) {
            // 스토리를 불러오는 경우
            PLAN_DATA_CHANGED -> {
                loadingDialog.dismiss()
                finish()
            }
        }
    }

    // Memo : BoundService 의 IBinder 객체를 받아와 현재 액티비티에서 서비스의 메소드를 사용하기 위한 클래스
    /*
    바운드 서비스에서는 HTTPConnectionThread(HandlerThread)가 동작하고 있으며, 이 스레드에 메시지를 통해 서버에 요청을 보낸다
    서버에서 결과를 보내주면 HTTPConnectionThread(HandlerThread)의 인터페이스 메소드 -> 바운드 서비스 -> 바운드 서비스 인터페이스 -> 액티비티 onHttpRespond 에서 handle 한다
     */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: HTTPConnectionService.LocalBinder = service as HTTPConnectionService.LocalBinder
            httpConnectionService = binder.getService()!!
            httpConnectionService.setOnHttpRespondListener(this@CalendarAddPlanActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}
