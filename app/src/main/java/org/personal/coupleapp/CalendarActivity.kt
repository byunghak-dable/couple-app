package org.personal.coupleapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import kotlinx.android.synthetic.main.activity_calendar.*
import org.personal.coupleapp.CalendarActivity.CustomHandler.Companion.GET_PLAN_DATA
import org.personal.coupleapp.adapter.ItemClickListener
import org.personal.coupleapp.adapter.PlanAdapter
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread
import org.personal.coupleapp.backgroundOperation.ServerConnectionThread.Companion.REQUEST_PLAN_DATA
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.utils.calendar.EventDecorator
import org.personal.coupleapp.utils.calendar.OneDayDecorator
import org.personal.coupleapp.utils.calendar.SaturdayDecorator
import org.personal.coupleapp.utils.calendar.SundayDecorator
import org.personal.coupleapp.utils.singleton.HandlerMessageHelper
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList


class CalendarActivity : AppCompatActivity(), View.OnClickListener, OnDateSelectedListener, ItemClickListener {

    private val TAG = javaClass.name

    private val serverPage = "Calendar"

    private lateinit var serverConnectionThread: ServerConnectionThread
    private val loadingDialog = LoadingDialog()
    private var isInitPlanData = false

    private var startCalendar: Calendar = Calendar.getInstance()
    private var startDateTime: Long? = null
    private var endDateTime: Long? = null

    // 현재 달의 모든 일정 리스트
    private val monthPlanList = ArrayList<PlanData>()
    // 선택한 날짜의 일정 리스트
    private val dayPlanList = ArrayList<PlanData>()
    private val planAdapter = PlanAdapter(dayPlanList, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        startWorkerThread()
        setListener()
        setCalendarRelatedWork()
        buildRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // 일정 데이터 요청
        if (!isInitPlanData) {
            val what = "getPlanData"
            val requestUrl = "$serverPage?what=$what&&startDateTime=$startDateTime&&endDateTime=$endDateTime"
            loadingDialog.show(supportFragmentManager, "LoadingDialog")
            HandlerMessageHelper.serverGetRequest(serverConnectionThread, requestUrl, GET_PLAN_DATA, REQUEST_PLAN_DATA)
            isInitPlanData = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorkerThread()
    }

    private fun setListener() {
        calendarCV.setOnDateChangedListener(this)
        addPlanBtn.setOnClickListener(this)
    }

    // 캘린더 관련 작업을 하는 메소드
    private fun setCalendarRelatedWork() {
        calendarCV.state().edit()
            .setFirstDayOfWeek(Calendar.SUNDAY)
            .setMinimumDate(CalendarDay.from(2017, 0, 1))
            .setMaximumDate(CalendarDay.from(2030, 11, 31))
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()

        calendarCV.addDecorators(SaturdayDecorator(), SundayDecorator(), OneDayDecorator())

        // 전달의 마지막 날 부터 다음 달의 첫날까지의 일정 데이터를 불러와서 캘린더에 보여준다
        val calendar = Calendar.getInstance()
        calendar[Calendar.DAY_OF_MONTH] = 0
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0

        // 전달 마지막 날의 timeInMills
        startDateTime = calendar.timeInMillis

        calendar[Calendar.MONTH] += 1

        // 다음 달 마지막 날의 timeInMills
        endDateTime = calendar.timeInMillis
    }

    // 백그라운드 스레드 실행
    private fun startWorkerThread() {
        val serverMainHandler = CustomHandler(this, loadingDialog, monthPlanList, dayPlanList, calendarCV, planAdapter)

        serverConnectionThread = ServerConnectionThread("ServerConnectionHelper", serverMainHandler)
        serverConnectionThread.start()
    }

    // 백그라운드의 루퍼를 멈춰줌으로써 스레드 종료
    private fun stopWorkerThread() {
        Log.i(TAG, "thread 종료")
        serverConnectionThread.looper.quit()
    }

    // 캘린더 일정 리사이클러 뷰 빌드
    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        planListRV.setHasFixedSize(true)
        planListRV.layoutManager = layoutManager
        planListRV.adapter = planAdapter
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addPlanBtn -> toAddPlan()
        }
    }

    private fun toAddPlan() {
        val toAddPlan = Intent(this, CalendarAddPlanActivity::class.java)
        val endCalendar = Calendar.getInstance()
        endCalendar[Calendar.HOUR] = startCalendar[Calendar.HOUR] + 6
        toAddPlan.putExtra("startCalendar", startCalendar)
        toAddPlan.putExtra("endCalendar", endCalendar)
        startActivity(toAddPlan)
    }

    // 캘린더 해당 날짜 클릭 리스너
    override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
        Log.i(TAG, "${date.day}")
        Log.i(TAG, "${startCalendar[Calendar.DAY_OF_MONTH]}")
        val calendar = Calendar.getInstance()
        dayPlanList.clear()
        monthPlanList.forEach {
            calendar.timeInMillis = it.start_time

            if (date.day == calendar[Calendar.DAY_OF_MONTH]) {
                dayPlanList.add(it)
            }
        }
        planAdapter.notifyDataSetChanged()
    }

    // 리사이클러 뷰 아이템 클릭 리스너
    override fun onItemClick(view: View?, itemPosition: Int) {

    }

    private class CustomHandler(
        activity: AppCompatActivity,
        loadingDialog: LoadingDialog,
        val monthPlanList: ArrayList<PlanData>,
        val dayPlanList: ArrayList<PlanData>,
        calendarCV: MaterialCalendarView,
        val planAdapter: PlanAdapter
    ) :
        Handler() {

        companion object {
            const val GET_PLAN_DATA = 1
        }

        private val TAG = javaClass.name

        private val activityWeakReference: WeakReference<AppCompatActivity> = WeakReference(activity)
        private val loadingDialogWeak: WeakReference<LoadingDialog> = WeakReference(loadingDialog)
        private val calendarCVWeak: WeakReference<MaterialCalendarView> = WeakReference(calendarCV)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val activity = activityWeakReference.get()

            if (activity != null) {
                when (msg.what) {
                    GET_PLAN_DATA -> {
                        loadingDialogWeak.get()?.dismiss()
                        if (msg.obj == null) {
                            Log.i(TAG, "데이터 더이상 없음")
                        } else {
                            val returnedData = msg.obj as HashMap<*, *>
                            val monthPlanList = returnedData["monthPlanList"] as ArrayList<PlanData>
                            val dayPlanList = returnedData["dayPlanList"] as ArrayList<PlanData>
                            val dates = returnedData["dates"] as ArrayList<CalendarDay>

                            monthPlanList.forEach { this.monthPlanList.add(it) }
                            dayPlanList.forEach{this.dayPlanList.add(it)}
                            calendarCVWeak.get()?.addDecorator(EventDecorator(activity, R.color.red, dates))
                            planAdapter.notifyDataSetChanged()
                            Log.i(TAG, "캘린더 테스트 : $dayPlanList" )
                        }
                    }
                }
            } else {
                return
            }
        }
    }
}



