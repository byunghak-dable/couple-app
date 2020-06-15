package org.personal.coupleapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.personal.coupleapp.R
import org.personal.coupleapp.data.PlanData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener
import org.personal.coupleapp.utils.singleton.CalendarHelper

class PlanAdapter(private val planList: ArrayList<PlanData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<PlanAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val startTimeTV: TextView = itemView.findViewById(R.id.startTimeTV)
        val endTimeTV: TextView = itemView.findViewById(R.id.endTimeTV)
        val typeImageIV: ImageView = itemView.findViewById(R.id.typeImageIV)
        val titleTV: TextView = itemView.findViewById(R.id.titleTV)
        val repeatBtn: ImageView = itemView.findViewById(R.id.repeatIV)
        val notificationIV: ImageView = itemView.findViewById(R.id.notificationIV)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {

            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemClick(itemView, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plan, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return planList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val planList: PlanData = planList[position]

        holder.startTimeTV.text = CalendarHelper.timeInMillsToTime(planList.start_time)
        holder.endTimeTV.text = CalendarHelper.timeInMillsToTime(planList.end_time)
        setTypeImage(planList.date_type, holder.typeImageIV)
        holder.titleTV.text = planList.title
        setRepeatImage(planList.repeat_type, holder.repeatBtn)
        setNotificationImage(planList.notification_time, holder.notificationIV)
    }

    private fun setTypeImage(dateType: String, typeImageIV: ImageView) {
        when (dateType) {
            "데이트" -> typeImageIV.setImageResource(R.drawable.ic_favorite_black_24dp)
            "여행" -> typeImageIV.setImageResource(R.drawable.ic_airplanemode_active_black_24dp)
            "문화생활" -> typeImageIV.setImageResource(R.drawable.ic_music_note_black_24dp)
            "학교" -> typeImageIV.setImageResource(R.drawable.ic_school_black_24dp)
            "기타" -> typeImageIV.setImageResource(R.drawable.ic_menu_black_24dp)
        }
    }

    private fun setRepeatImage(repeatType: String, repeatBtn: ImageView) {
        when (repeatType) {
            "안함" -> return
            else -> repeatBtn.setImageResource(R.drawable.ic_repeat_black_24dp)
        }
    }

    private fun setNotificationImage(notificationTime: String?, notificationIV: ImageView) {
        when (notificationTime) {
            "안함" -> return
            else -> notificationIV.setImageResource(R.drawable.ic_notifications_black_24dp)
        }
    }
}