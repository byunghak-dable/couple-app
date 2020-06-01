package org.personal.coupleapp.utils.singleton

import java.text.DateFormat
import java.util.*

object CalendarHelper {

    private val calendar = Calendar.getInstance()

    // 시간과 분을 입력 받고 사용자가 보기 편하게 시간 텍스트로 보여주는 메소드
    fun setTimeFormat(hour: Int, minute: Int): String {
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
        calendar[Calendar.SECOND] = 0

        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    // 년도, 월, 일을 입력 받고 Date 을 사용자에게 텍스트로 보여주는 메소드
    fun setDateFormat(year: Int, month: Int, dayOfMonth: Int): String {
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        return DateFormat.getDateInstance().format(calendar.time)
    }

    fun dateToTimeInMills(year: Int, month: Int, dayOfMonth: Int): Int {
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        return calendar.timeInMillis.toInt()
    }

    //------------------ 현재 날짜를 가져오는 메소드들 ------------------

    fun getCurrentHour(): Int {
        return calendar.get(Calendar.HOUR)
    }

    fun getCurrentMinute(): Int {
        return calendar.get(Calendar.MINUTE)
    }

    fun getCurrentYear(): Int {
        return calendar.get(Calendar.YEAR)
    }

    fun getCurrentMonth(): Int {
        return calendar.get(Calendar.MONTH)
    }

    fun getCurrentDay(): Int {
        return calendar.get(Calendar.DAY_OF_MONTH)
    }
}