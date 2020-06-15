package org.personal.coupleapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.personal.coupleapp.R
import org.personal.coupleapp.data.OpenChatRoomData
import org.personal.coupleapp.interfaces.recyclerView.ItemClickListener

class OpenChatRoomAdapter(private val context: Context, private val openChatRoomList: ArrayList<OpenChatRoomData>, private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<OpenChatRoomAdapter.ViewHolder>() {

    private val TAG = javaClass.name

    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {


        val titleTV: TextView = itemView.findViewById(R.id.titleTV)
        val participantCountTV: TextView = itemView.findViewById(R.id.participantCountTV)
        val descriptionTV: TextView = itemView.findViewById(R.id.descriptionTV)
        val roomCoverImageIV: ImageView = itemView.findViewById(R.id.roomProfileImageIV)


        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_open_chat, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return openChatRoomList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val openChatRoomData: OpenChatRoomData = openChatRoomList[position]
        Log.i(TAG, openChatRoomData.toString())
        holder.titleTV.text  = openChatRoomData.name
        holder.descriptionTV.text = openChatRoomData.description
        holder.participantCountTV.text = String.format("참여인원 %s명", openChatRoomData.participants_count)
        Glide.with(context).load(openChatRoomData.cover_image).into(holder.roomCoverImageIV)
    }
}