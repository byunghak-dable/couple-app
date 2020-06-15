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
import org.personal.coupleapp.data.ChatData


class ChatAdapter(val context: Context, private val myUserID: Int, private val messageList: ArrayList<ChatData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TAG = javaClass.name

    private val MY_CHAT = 1
    private val OTHER_CHAT = 2

    class OthersChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageIV: ImageView = itemView.findViewById(R.id.profileIV)
        val nameTV: TextView = itemView.findViewById(R.id.nameTV)
        val otherMessageTV: TextView = itemView.findViewById(R.id.messageTV)
        val otherMessageTimeTV: TextView = itemView.findViewById(R.id.messageTimeTV)
    }

    class MyChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val myMessageTV: TextView = itemView.findViewById(R.id.messageTV)
        val myMessageTimeTV: TextView = itemView.findViewById(R.id.messageTimeTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View

        if (viewType == MY_CHAT) {
            view = inflater.inflate(R.layout.item_my_chat, parent, false)
            return MyChatViewHolder(view)
        }
        view = inflater.inflate(R.layout.item_others_chat, parent, false)
        return OthersChatViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        if (myUserID == messageList[position].sender_id) {
            return MY_CHAT
        }
        return OTHER_CHAT
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val messageData = messageList[position]

        if (myUserID == messageData.sender_id) {
            val myChatViewHolder = holder as MyChatViewHolder
            myChatViewHolder.myMessageTV.text  = messageData.message
            myChatViewHolder.myMessageTimeTV.text = messageData.message_time
        } else {
            val othersChatViewHolder = holder as OthersChatViewHolder
            Glide.with(context).load(messageData.profile_image).into(othersChatViewHolder.profileImageIV)
            othersChatViewHolder.nameTV.text = messageData.name
            othersChatViewHolder.otherMessageTV.text = messageData.message
            othersChatViewHolder.otherMessageTimeTV.text = messageData.message_time
        }
    }
}