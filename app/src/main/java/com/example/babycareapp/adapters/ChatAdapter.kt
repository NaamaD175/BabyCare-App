package com.example.babycareapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.babycareapp.R
import com.example.babycareapp.models.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<Message>, //List of the messages
    private val currentUserId: String //The current ID of the current user
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1 //message from me (the current user)
        private const val VIEW_TYPE_RECEIVED = 2 //message that received from another user
    }

    //To know what is the type of the message - sent or received
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    //To create the correct ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    //Fill the chat card with the correct details
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(message.timestamp))

        //Different between sent or received
        if (holder is SentMessageViewHolder) {
            holder.messageText.text = message.text
            holder.messageTime.text = formattedTime
        } else if (holder is ReceivedMessageViewHolder) {
            holder.messageText.text = message.text
            holder.messageTime.text = formattedTime
        }
    }

    //To know how many chats we have
    override fun getItemCount(): Int = messages.size

    //Display of sent message
    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.text_message_body)
        val messageTime: TextView = view.findViewById(R.id.text_message_time)
    }

    //Display of received message
    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.text_message_body)
        val messageTime: TextView = view.findViewById(R.id.text_message_time)
    }

}