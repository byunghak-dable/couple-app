package org.personal.coupleapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.models.SlideModel
import org.personal.coupleapp.R
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.interfaces.ItemClickListener
import org.personal.coupleapp.utils.singleton.CalendarHelper

class StoryAdapter(private val storyList: ArrayList<StoryData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val imageSlider: ImageSlider = itemView.findViewById(R.id.imageSliderIS)
        val titleTV: TextView = itemView.findViewById(R.id.titleTV)
        val timeTV: TextView = itemView.findViewById(R.id.timeTV)
        val descriptionTV: TextView = itemView.findViewById(R.id.descriptionTV)
        private val settingBtn: ImageButton = itemView.findViewById(R.id.settingBtn)

        init {
            settingBtn.setOnClickListener(this)
        }

        override fun onClick(v: View?) {

            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemClick(settingBtn, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itme_home_story, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return storyList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val storyData: StoryData = storyList[position]
        val slideModelList = ArrayList<SlideModel>()

        // 스토리 데이터 중 이미지 정보는 url String 값을 받아오기 때문에 SlideModel 객체를 생성해서 추가한다.
        storyData.photo_path.forEach {
            val slideModel = SlideModel(it as String)
            slideModelList.add(slideModel)
        }

        holder.imageSlider.setImageList(slideModelList, true)
        holder.titleTV.text = storyData.title
        holder.timeTV.text = CalendarHelper.timeInMillsToDate(storyData.date)
        holder.descriptionTV.text = storyData.description
    }
}