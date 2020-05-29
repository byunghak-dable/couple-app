package org.personal.coupleapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_calendar_add_plan.*
import org.personal.coupleapp.dialog.*
import org.personal.coupleapp.utils.singleton.CalendarHelper

class CalendarAddPlanActivity : AppCompatActivity(), View.OnClickListener, PlanTypeDialog.DialogListener, TimePickerDialog.TimePickerListener,
    AlertDialog.DialogListener, DatePickerDialog.DatePickerListener , RadioButtonDialog.DialogListener{

    private val TAG = javaClass.name

    // utils/singleton java.util 캘린더 객체를 사용하는 메소드 모아둔 싱글톤
    private val calendarHelper = CalendarHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_add_plan)
        setListener()
    }

    override fun onBackPressed() {
        val warningDialog = AlertDialog()
        val arguments = Bundle()

        arguments.putString("title", getText(R.string.goBackTitle).toString())
        arguments.putString("message", getText(R.string.goBackMessage).toString())

        warningDialog.arguments = arguments
        warningDialog.show(supportFragmentManager, "BackWarning")
    }

    private fun setListener() {
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

    //------------------ 클릭 시 이벤트 관리하는 메소드 모음 ------------------
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.publicTV -> applyPlanClassification(R.color.green, R.color.black, R.color.mainBackground)
            R.id.privateTV -> applyPlanClassification(R.color.black, R.color.red, R.color.red)
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

    // 공동 / 개인 선택 시 변화 적용
    private fun applyPlanClassification(publicColor: Int, privateColor: Int, imageColor: Int) {
        publicTV.setTextColor(getColor(publicColor))
        privateTV.setTextColor(getColor(privateColor))
        classificationIV.backgroundTintList = getColorStateList(imageColor)
        overlapClassificationIV.backgroundTintList = getColorStateList(imageColor)
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
            "startDate" -> applyDateChoice(startDateBtn, year, month, dayOfMonth)
            "endDate" -> applyDateChoice(endDateBtn, year, month, dayOfMonth)
        }
    }

    // 시간(시작, 종료) 선택 시
    override fun onTimeSet(timePicker: TimePicker?, hour: Int, minute: Int, whichPicker: String?) {
        when (whichPicker) {
            "startTime" -> applyTimeChoice(startTimeBtn, hour, minute)
            "endTime" -> applyTimeChoice(endTimeBtn, hour, minute)
        }
    }

    // 반복, 알림 선택 시
    override fun onRadioBtnChoice(whichDialog: Int, choice: String) {
        when(whichDialog) {
            R.array.repeat -> repeatBtn.text = choice
            R.array.notification -> notificationBtn.text = choice
        }
    }

    // TODO: date 포맷을다시 year, date, dayOfMonth 로 변환 가능한지 찾는 중
    private fun applyDateChoice(dateBtn:Button, year:Int, month:Int, dayOfMonth: Int) {
        dateBtn.text = calendarHelper.setDateFormat(year, month, dayOfMonth)
    }

    // TODO: time 포맷을다시 hour, minute 으로 변환 가능한지 찾는 중
    private fun applyTimeChoice(timeBtn: Button, hour: Int, minute: Int) {
        // 시스템에서 설정된 시간 표현법으로 시간을 출력한다
        timeBtn.text = calendarHelper.setTimeFormat(hour, minute)
    }

    // alert 다이얼로그 확인 버튼 누를 때
    override fun applyConfirm() {
        finish()
    }
}
