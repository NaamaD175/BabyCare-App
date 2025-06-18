package com.example.babycareapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.babycareapp.adapters.ChatAdapter
import com.example.babycareapp.databinding.FragmentChatBinding
import com.example.babycareapp.models.Babysitter
import com.example.babycareapp.models.Message
import com.example.babycareapp.utils.generateChatId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.example.babycareapp.R

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var babysitter: Babysitter? = null
    private lateinit var chatId: String
    private lateinit var databaseRef: DatabaseReference
    private val messageList = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter
    private lateinit var otherUserId: String

    private lateinit var messagesListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (requireArguments().containsKey("babysitter")) {
            babysitter = requireArguments().getSerializable("babysitter") as Babysitter
            otherUserId = babysitter!!.uploaderId
        } else {
            otherUserId = requireArguments().getString("otherUserId") ?: ""
        }

        chatId = generateChatId(currentUserId, otherUserId)

        Log.d("ChatDebug", "chatId: $chatId")

        databaseRef = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(chatId)
            .child("messages")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        binding.chatIMGBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_FRAME_container, MessagesFragment())
                .commit()
        }

        adapter = ChatAdapter(messageList, currentUserId)
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = adapter

        if (babysitter != null) {
            binding.chatLBLName.text = babysitter!!.name
            Glide.with(requireContext()).load(babysitter!!.imageUrl)
                .into(binding.chatIMGBabysitter)
        } else {
            FirebaseFirestore.getInstance().collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener { doc ->
                    val email = doc.getString("email") ?: "Unknown user"
                    binding.chatLBLName.text = email
                }
                .addOnFailureListener {
                    binding.chatLBLName.text = "Unknown user"
                }

            binding.chatIMGBabysitter.setImageResource(R.drawable.ic_person_placeholder)
        }

        binding.chatSendButton.setOnClickListener {
            val text = binding.chatEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                databaseRef.push().setValue(message)
                binding.chatEditText.setText("")
            }
        }

        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnap in snapshot.children) {
                    val msg = msgSnap.getValue(Message::class.java)
                    if (msg != null && (msg.senderId == currentUserId || msg.receiverId == currentUserId)) {
                        messageList.add(msg)
                    }
                }

                _binding?.let { binding ->
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        }

        databaseRef.addValueEventListener(messagesListener)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        databaseRef.removeEventListener(messagesListener)
    }

    companion object {
        fun newInstance(babysitter: Babysitter): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putSerializable("babysitter", babysitter)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceWithUid(otherUserId: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString("otherUserId", otherUserId)
            fragment.arguments = args
            return fragment
        }
    }
}
