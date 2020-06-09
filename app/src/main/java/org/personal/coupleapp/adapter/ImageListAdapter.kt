package org.personal.coupleapp.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.personal.coupleapp.R


class ImageListAdapter(private val imageList: ArrayList<Bitmap?>) : RecyclerView.Adapter<ImageListAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIV: ImageView = itemView.findViewById(R.id.imageIV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageBitmap: Bitmap? = imageList[position]
        holder.imageIV.setImageBitmap(imageBitmap)
    }
}