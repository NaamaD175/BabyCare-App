package com.example.babycareapp.models

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)

