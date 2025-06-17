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
    private val chatIds: List<String>,
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
        val parts = chatId.split("_")
        if (parts.size != 2) {
            holder.chatName.text = "Invalid chat"
            return
        }

        val otherUserId = if (parts[0] == currentUserId) parts[1] else parts[0]
        val db = FirebaseFirestore.getInstance()

        // ננסה קודם למצוא babysitter
        db.collection("babysitters")
            .whereEqualTo("uploaderId", otherUserId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    val babysitter = doc.toObject(Babysitter::class.java)
                    holder.chatName.text = babysitter?.name ?: "Unknown"

                    holder.itemView.setOnClickListener {
                        val fragment = ChatFragment.newInstance(babysitter!!)
                        val activity = holder.itemView.context as androidx.fragment.app.FragmentActivity
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.main_FRAME_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                } else {
                    // אם אין בייביסיטר – ננסה להביא את האימייל מהמסמך users/{uid}
                    db.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val email = userDoc.getString("email") ?: "Unknown user"
                            holder.chatName.text = email

                            holder.itemView.setOnClickListener {
                                val fragment = ChatFragment.newInstanceWithUid(otherUserId)
                                val activity = holder.itemView.context as androidx.fragment.app.FragmentActivity
                                activity.supportFragmentManager.beginTransaction()
                                    .replace(R.id.main_FRAME_container, fragment)
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }
                        .addOnFailureListener {
                            holder.chatName.text = "Unknown user"
                        }
                }
            }
            .addOnFailureListener {
                holder.chatName.text = "Failed to load chat"
            }
    }



    override fun getItemCount(): Int = chatIds.size
}
