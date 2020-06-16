package org.personal.coupleapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.personal.coupleapp.R
import org.personal.coupleapp.data.AlbumFolderData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener

class AlbumFolderAdapter(val context: Context, private val albumList: ArrayList<AlbumFolderData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<AlbumFolderAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        val folderTitleTV:TextView = itemView.findViewById(R.id.folderTitleTV)
        val contentCountTV:TextView  = itemView.findViewById(R.id.contentCountTV)
        val folderImageIV : ImageView = itemView.findViewById(R.id.folderImageIV)

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album_folder, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val albumFolderData = albumList[position]
        holder.folderTitleTV.text = albumFolderData.name
        holder.contentCountTV.text = String.format("폴더 내 이미지 : %s개", albumFolderData.image_count.toString())
        if (albumFolderData.representative_image != null) {
            Glide.with(context).load(albumFolderData.representative_image).into(holder.folderImageIV)
        }
    }
}