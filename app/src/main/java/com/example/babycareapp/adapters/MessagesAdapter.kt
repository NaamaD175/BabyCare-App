package com.example.babycareapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.babycareapp.R
import com.example.babycareapp.fragments.ChatFragment
import com.example.babycareapp.models.Babysitter
import com.google.firebase.firestore.FirebaseFirestore

class MessagesAdapter(
    private val chatIds: List<String>, //List of the chats ID - knows which message belongs to which chat
    private val currentUserId: String
) : RecyclerView.Adapter<MessagesAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatName: TextView = itemView.findViewById(R.id.chat_item_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatId = chatIds[position]
        //Check to see if there are 2 participants in the chat
        val parts = chatId.split("_")
        if (parts.size != 2) {
            holder.chatName.text = "Invalid chat"
            return
        }
        //Get the other user that is participant in this chat
        val otherUserId = if (parts[0] == currentUserId) parts[1] else parts[0]
        val db = FirebaseFirestore.getInstance()

        //Search if the other user is also a babysitter
        db.collection("babysitters")
            .whereEqualTo("uploaderId", otherUserId)
            .get()
            .addOnSuccessListener { docs ->
                //If its a babysitter - we take her name
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    val babysitter = doc.toObject(Babysitter::class.java)
                    holder.chatName.text = babysitter?.name ?: "Unknown"
                    //When we click on the name the chat fragment will open
                    holder.itemView.setOnClickListener {
                        val fragment = ChatFragment.newInstance(babysitter!!)
                        val activity = holder.itemView.context as androidx.fragment.app.FragmentActivity
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.main_FRAME_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                //If its not a babysitter we take the name from the users collection
                } else {
                    db.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { userDoc ->
                            //If we found him we take the email else we will write Unknown user
                            val email = userDoc.getString("email") ?: "Unknown user"
                            holder.chatName.text = email
                            //When we click on the name the chat fragment will open
                            holder.itemView.setOnClickListener {
                                val fragment = ChatFragment.newInstanceWithUid(otherUserId)
                                val activity = holder.itemView.context as androidx.fragment.app.FragmentActivity
                                activity.supportFragmentManager.beginTransaction()
                                    .replace(R.id.main_FRAME_container, fragment)
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }
                        //If the users collection failed - unknown user
                        .addOnFailureListener {
                            holder.chatName.text = "Unknown user"
                        }
                }
            }
            //If there any problem - Failed to load chat
            .addOnFailureListener {
                holder.chatName.text = "Failed to load chat"
            }
    }

    //The number of messages
    override fun getItemCount(): Int = chatIds.size
}
