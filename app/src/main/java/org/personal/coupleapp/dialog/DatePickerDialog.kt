package org.personal.coupleapp.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import org.personal.coupleapp.utils.singleton.CalendarHelper

class DatePickerDialog : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private val TAG = javaClass.name

    private lateinit var datePickerListener: DatePickerListener
    private var whichPicker:String? = null

    // utils/singleton java.util 캘린더 객체를 사용하는 메소드 모아둔 싱글톤
    private val calendarHelper = CalendarHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        whichPicker = arguments?.getString("whichPicker")

        val initYear = calendarHelper.getCurrentYear()
        val initMonth = calendarHelper.getCurrentMonth()
        val initDay = calendarHelper.getCurrentDay()

        return DatePickerDialog(activity as Context, this, initYear, initMonth, initDay)
    }

    // 다이얼로그에서 액티비티로 데이터 전송
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {

            datePickerListener = context as DatePickerListener

        } catch (e: ClassCastException) {

            e.printStackTrace()
            Log.i(TAG, "onAttach : 인터페이스 implement 안함")
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        datePickerListener.applyDateSet(view, year, month, dayOfMonth, whichPicker)
    }

    interface DatePickerListener {
        fun applyDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int, whichPicker: String?)
    }
}