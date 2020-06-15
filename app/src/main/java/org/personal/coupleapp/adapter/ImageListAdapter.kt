package org.personal.coupleapp.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.personal.coupleapp.R
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener


class ImageListAdapter(private val imageList: ArrayList<Bitmap?>, private val itemClickListener:ItemClickListener) : RecyclerView.Adapter<ImageListAdapter.ViewHolder>() {


    class ViewHolder(itemView: View, private val itemClickListener:ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {
        init{
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }
        val imageIV: ImageView = itemView.findViewById(R.id.imageIV)

        override fun onClick(v: View?) {
            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemClick(itemView, adapterPosition)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemLongClick(itemView, adapterPosition)
            }
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_list, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageBitmap: Bitmap? = imageList[position]
        holder.imageIV.setImageBitmap(imageBitmap)
    }
}