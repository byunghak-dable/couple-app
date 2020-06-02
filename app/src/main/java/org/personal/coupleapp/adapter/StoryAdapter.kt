package org.personal.coupleapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.models.SlideModel
import org.personal.coupleapp.R
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.utils.singleton.CalendarHelper

class StoryAdapter(private val storyList: ArrayList<StoryData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    val VIEW_TYPE_LOADING = 1
    val VIEW_TYPE_ITEM = 2

    class ViewHolder(itemView: View, val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val imageSlider: ImageSlider = itemView.findViewById(R.id.imageSliderIS)
        val titleTV : TextView = itemView.findViewById(R.id.titleTV)
        val timeTV : TextView = itemView.findViewById(R.id.timeTV)
        val descriptionTV : TextView = itemView.findViewById(R.id.descriptionTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itme_home_story, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return storyList.size
    }


    override fun getItemViewType(position: Int): Int {

        return if (storyList[position] == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onBindViewHolder(holder: StoryAdapter.ViewHolder, position: Int) {
        val storyData: StoryData = storyList[position]
        val slideModelList = ArrayList<SlideModel>()

        storyData.photo_path.forEach{
            val slideModel = SlideModel(it as String)
            slideModelList.add(slideModel)
        }

        holder.imageSlider.setImageList(slideModelList, true)
        holder.titleTV.text = storyData.title
        holder.timeTV.text = CalendarHelper.timeInMillsToDate(storyData.date)
        holder.descriptionTV.text = storyData.description
    }
}