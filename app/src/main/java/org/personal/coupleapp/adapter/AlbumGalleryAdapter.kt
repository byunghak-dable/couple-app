package org.personal.coupleapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.personal.coupleapp.R
import org.personal.coupleapp.data.AlbumGalleryData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener

class AlbumGalleryAdapter(val context: Context, private val galleryList: ArrayList<AlbumGalleryData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<AlbumGalleryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener {

        init {
            itemView.setOnLongClickListener(this)
        }

        val storyImageIV: ImageView = itemView.findViewById(R.id.storyImageIV)

        override fun onLongClick(v: View?): Boolean {
            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemLongClick(itemView, adapterPosition)
            }
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return galleryList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val galleryData = galleryList[position]
        Glide.with(context).load(galleryData.image_url).into(holder.storyImageIV)
    }
}