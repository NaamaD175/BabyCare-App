package com.example.babycareapp.utils

fun generateChatId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
}

