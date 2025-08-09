package com.example.babycareapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.babycareapp.R
import com.example.babycareapp.adapters.MessagesAdapter
import com.example.babycareapp.utils.generateChatId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessagesFragment : Fragment() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    private val chatIds = mutableListOf<String>()
    private lateinit var adapter: MessagesAdapter
    private lateinit var databaseRef: DatabaseReference
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        messagesRecyclerView = view.findViewById(R.id.messages_RV_list)
        emptyTextView = view.findViewById(R.id.messages_empty_text)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MessagesAdapter(chatIds, currentUserId)
        messagesRecyclerView.adapter = adapter

        loadChats()
        return view
    }

    //Function to retrieve all chats that include the current user
    private fun loadChats() {
        databaseRef = FirebaseDatabase.getInstance().getReference("chats")

        Log.d("MessagesFragment", "Current userId: $currentUserId")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatIds.clear()

                for (chatSnap in snapshot.children) {
                    val chatId = chatSnap.key ?: continue
                    val messagesSnap = chatSnap.child("messages")

                    for (messageSnap in messagesSnap.children) {
                        val msg = messageSnap.getValue(com.example.babycareapp.models.Message::class.java)
                        if (msg != null) {
                            if (msg.senderId == currentUserId || msg.receiverId == currentUserId) {
                                chatIds.add(chatId)
                                break
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged()
                emptyTextView.visibility = if (chatIds.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                //Show error message if database retrieval fails
                emptyTextView.text = "Error loading chats"
                emptyTextView.visibility = View.VISIBLE
            }
        })
    }

}
