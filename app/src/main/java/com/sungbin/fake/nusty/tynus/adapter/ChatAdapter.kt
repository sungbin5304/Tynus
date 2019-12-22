package com.sungbin.fake.nusty.tynus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sungbin.fake.nusty.tynus.R
import com.sungbin.fake.nusty.tynus.dto.ChatItem

class ChatAdapter(private val list: ArrayList<ChatItem>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var msg_L: TextView = view.findViewById(R.id.msg_L)
        var msg_R: TextView = view.findViewById(R.id.msg_R)

        var content_L: CardView = view.findViewById(R.id.content_L)
        var content_R: CardView = view.findViewById(R.id.content_R)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_chat_layout, viewGroup, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull viewholder: ChatViewHolder, position: Int) {
        val isMyChat = list[position].isMyChat!!
        val content = list[position].content

        if(isMyChat){ //채팅 오른쪽
            viewholder.content_L.visibility = View.GONE
            viewholder.content_R.visibility = View.VISIBLE
            viewholder.msg_R.text = content
        }
        else { //채팅 왼쪽
            viewholder.content_R.visibility = View.GONE
            viewholder.content_L.visibility = View.VISIBLE
            viewholder.msg_L.text = content
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getItem(position: Int): ChatItem {
        return list[position]
    }
}
