package org.personal.coupleapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import kotlinx.android.synthetic.main.activity_calendar.*
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.adapter.PlanAdapter
import org.personal.coupleapp.backgroundOperation.HTTPConnectionThread.Companion.REQUEST_PLAN_DATA
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.dialog.LoadingDialog
import org.personal.coupleapp.interfaces.service.HTTPConnectionListener
import org.personal.coupleapp.service.HTTPConnectionService
import org.personal.coupleapp.utils.calendar.EventDecorator
import org.personal.coupleapp.utils.calendar.OneDayDecorator
import org.personal.coupleapp.utils.calendar.SaturdayDecorator
import org.personal.coupleapp.utils.calendar.SundayDecorator
import org.personal.coupleapp.utils.singleton.SharedPreferenceHelper
import java.util.*
import kotlin.collections.ArrayList


class CalendarActivity : AppCompatActivity(), View.OnClickListener, OnDateSelectedListener,
    ItemClickListener,
    HTTPConnectionListener {

    private val TAG = javaClass.name

    private val serverPage = "Calendar"

    private lateinit var httpConnectionService: HTTPConnectionService
    private val GET_PLAN_DATA = 1

    private val loadingDialog = LoadingDialog()

    private var startCalendar: Calendar = Calendar.getInstance()
    private var startDateTime: Long? = null

    // 현재 달의 모든 일정 리스트
    private val monthPlanList = ArrayList<PlanData>()

    // 선택한 날짜의 일정 리스트
    private val dayPlanList = ArrayList<PlanData>()
    private val planAdapter = PlanAdapter(dayPlanList, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        setListener()
        setCalendarRelatedWork()
        buildRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        startBoundService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun setListener() {
        calendarCV.setOnDateChangedListener(this)
        addPlanBtn.setOnClickListener(this)
    }

    // 캘린더 관련 초반 캘린더 설정을 하는 메소드
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
    }

    // 캘린더 일정 리사이클러 뷰 빌드
    private fun buildRecyclerView() {
        val layoutManager = LinearLayoutManager(this)

        planListRV.setHasFixedSize(true)
        planListRV.layoutManager = layoutManager
        planListRV.adapter = planAdapter
    }

    // 현재 액티비티와 HTTPConnectionService(Bound Service)를 연결하는 메소드
    private fun startBoundService() {
        val startService = Intent(this, HTTPConnectionService::class.java)
        bindService(startService, connection, BIND_AUTO_CREATE)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addPlanBtn -> toAddPlan()
        }
    }

    // 일정 추가하기 버튼을 눌렀을 때
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

    override fun onItemLongClick(view: View?, itemPosition: Int) {
    }

    // http 바인드 서비스 인터페이스 메소드
    override fun onHttpRespond(responseData: HashMap<*, *>) {
        val handler = Handler(Looper.getMainLooper())
        when (responseData["whichRespond"] as Int) {
            GET_PLAN_DATA -> {
                Log.i(TAG, "onHttpRespond : 일정 데이터 가져오기")
                loadingDialog.dismiss()

                when (responseData["whichRespond"]) {
                    null -> {
                        Log.i(TAG, "캘린더 : 데이터 더이상 없음")
                    }
                    else -> {
                        val returnedData = responseData["respondData"] as HashMap<*, *>
                        val monthPlanList = returnedData["monthPlanList"] as ArrayList<PlanData>
                        val dayPlanList = returnedData["dayPlanList"] as ArrayList<PlanData>
                        val dates = returnedData["dates"] as ArrayList<CalendarDay>

                        monthPlanList.forEach { this.monthPlanList.add(it) }
                        dayPlanList.forEach { this.dayPlanList.add(it) }

                        handler.post {
                            calendarCV.addDecorator(EventDecorator(this, R.color.red, dates))
                            planAdapter.notifyDataSetChanged()
                        }
                        Log.i(TAG, "캘린더 view : $dayPlanList")
                    }
                }
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
            httpConnectionService.setOnHttpRespondListener(this@CalendarActivity)

            val coupleID by lazy { SharedPreferenceHelper.getInt(this@CalendarActivity, getText(R.string.coupleColumnID).toString()) }
            val what = "getPlanData"
            val requestUrl = "$serverPage?what=$what&&coupleID=$coupleID&&startDateTime=$startDateTime"
            loadingDialog.show(supportFragmentManager, "LoadingDialog")
            dayPlanList.clear()
            monthPlanList.clear()
            httpConnectionService.serverGetRequest(requestUrl, REQUEST_PLAN_DATA, GET_PLAN_DATA)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "바운드 서비스 연결 종료")
        }
    }
}



