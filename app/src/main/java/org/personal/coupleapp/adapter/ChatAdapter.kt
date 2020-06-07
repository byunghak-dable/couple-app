package org.personal.coupleapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.personal.coupleapp.R
import org.personal.coupleapp.data.CoupleChatData


class ChatAdapter(val context: Context, private val myUserID: Int, private val messageList: ArrayList<CoupleChatData>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val TAG = javaClass.name

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileCV:CardView = itemView.findViewById(R.id.profileCV)
        val profileImageIV: ImageView = itemView.findViewById(R.id.profileIV)
        val nameTV: TextView = itemView.findViewById(R.id.nameTV)
        val messageTV: TextView = itemView.findViewById(R.id.messageTV)
        val itemLayout: ConstraintLayout = itemView.findViewById(R.id.chattingItemCL)
        val messageTimeTV: TextView = itemView.findViewById(R.id.messageTimeTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chatting, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatData: CoupleChatData = messageList[position]
        val constraintSet = ConstraintSet()
        constraintSet.clone(holder.itemLayout)
        Log.i(TAG, chatData.toString())
        // 채팅 메시지 넣기
        holder.messageTV.text = chatData.message
        holder.messageTimeTV.text = chatData.message_time

        if (chatData.sender_id == myUserID) {
            // 레이아웃 변경
            constraintSet.clear(holder.messageTV.id, ConstraintSet.START)
            constraintSet.clear(holder.messageTimeTV.id, ConstraintSet.END)
            constraintSet.connect(holder.messageTV.id, ConstraintSet.END, holder.itemLayout.id, ConstraintSet.END)
            constraintSet.connect(holder.messageTimeTV.id, ConstraintSet.END, holder.messageTV.id, ConstraintSet.START)
            constraintSet.applyTo(holder.itemLayout)

        }else {
            // 프로필 이미지, 이름 넣기
            holder.profileCV.visibility = View.VISIBLE
            holder.nameTV.visibility = View.VISIBLE

            Glide.with(context).load(chatData.profile_image).into(holder.profileImageIV)
            holder.nameTV.text = chatData.name
        }
    }
}