package org.personal.coupleapp.utils.singleton

import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

object CalendarHelper {


    val timeList: ArrayList<Long> = ArrayList()

    // 시간과 분을 입력 받고 사용자가 보기 편하게 시간 텍스트로 보여주는 메소드
    fun setTimeFormat(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
        calendar[Calendar.SECOND] = 0

        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    // 년도, 월, 일을 입력 받고 Date 을 사용자에게 텍스트로 보여주는 메소드
    fun setDateFormat(year: Int, month: Int, dayOfMonth: Int): String {
        val calendar = Calendar.getInstance()
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        return DateFormat.getDateInstance().format(calendar.time)
    }

    // 날짜를 밀리세컨으로 변환하는 메소드
    fun dateToTimeInMills(year: Int, month: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance()
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        return calendar.timeInMillis
    }

    fun timeInMillsToDate(timeInMills: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMills

        return DateFormat.getDateInstance().format(calendar.time)
    }

    fun timeInMillsToTime(timeInMills: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMills

        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    //------------------ 현재 날짜를 가져오는 메소드들 ------------------

    fun getCurrentHour(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR)
    }

    fun getCurrentMinute(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MINUTE)
    }

    fun getCurrentYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR)
    }

    fun getCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH)
    }

    fun getCurrentDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_MONTH)
    }
}