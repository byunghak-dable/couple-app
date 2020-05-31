package org.personal.coupleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_calendar.*

class CalendarActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        setListener()
    }

    private fun setListener() {
        addPlanBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.addPlanBtn -> toAddPlan()
        }
    }

    private fun toAddPlan() {
        val toAddPlan = Intent(this, CalendarAddPlanActivity::class.java)
        startActivity(toAddPlan)
    }
}
