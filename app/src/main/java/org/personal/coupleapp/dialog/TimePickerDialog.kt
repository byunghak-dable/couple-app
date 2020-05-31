package org.personal.coupleapp.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import org.personal.coupleapp.utils.singleton.CalendarHelper

class TimePickerDialog : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private val TAG = javaClass.name

    private lateinit var timePickerListener: TimePickerListener
    private var whichPicker: String? = null

    // utils/singleton java.util 캘린더 객체를 사용하는 메소드 모아둔 싱글톤
    private val calendarHelper = CalendarHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // 타임피커 다이얼로그가 여러 개 필요할 때 구분하기 위한 변수(사용하지 않을 때는 null)
        whichPicker = arguments?.getString("whichTimePicker")

        val initHour = calendarHelper.getCurrentHour()
        val initMinute = calendarHelper.getCurrentMinute()

        return TimePickerDialog(activity, this, initHour, initMinute, DateFormat.is24HourFormat(context))
    }

    // 다이얼로그에서 액티비티로 데이터 전송
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {

            timePickerListener = context as TimePickerListener

        } catch (e: ClassCastException) {

            e.printStackTrace()
            Log.i(TAG, "onAttach : 인터페이스 implement 해야 함")
        }
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {

        timePickerListener.onTimeSet(view, hourOfDay, minute, whichPicker)
    }

    interface TimePickerListener {
        fun onTimeSet(timePicker: TimePicker?, hour: Int, minute: Int, whichPicker: String?)
    }
}