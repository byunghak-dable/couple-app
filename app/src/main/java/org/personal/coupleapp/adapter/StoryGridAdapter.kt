package org.personal.coupleapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.personal.coupleapp.R
import org.personal.coupleapp.data.StoryData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener

class StoryGridAdapter(val context: Context, private val storyList: ArrayList<StoryData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<StoryGridAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val storyImage: ImageView = itemView.findViewById(R.id.storyImageIV)

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return storyList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val storyData: StoryData = storyList[position]
        Glide.with(context).load(storyData.photo_path[0]).into(holder.storyImage)
    }
}