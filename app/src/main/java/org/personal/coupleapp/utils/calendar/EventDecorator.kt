package org.personal.coupleapp.utils.calendar

import androidx.appcompat.app.AppCompatActivity
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan


class EventDecorator(val activity: AppCompatActivity, val color: Int, private val dates: Collection<CalendarDay?>?) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates!!.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(5.0f, color))
    }
}