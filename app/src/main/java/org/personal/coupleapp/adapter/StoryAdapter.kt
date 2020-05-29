package org.personal.coupleapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import org.personal.coupleapp.R
import org.personal.coupleapp.data.StoryData

class StoryAdapter(private val storyList: ArrayList<StoryData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<StoryAdapter.ViewHolder>() {


    class ViewHolder(itemView: View, val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val imageSlider: ImageSlider = itemView.findViewById(R.id.imageSliderIS)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itme_story, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return storyList.size
    }

    override fun onBindViewHolder(holder: StoryAdapter.ViewHolder, position: Int) {
        val storyData: StoryData = storyList[position]
        holder.imageSlider.setImageList(storyData.imageList, true)
    }
}