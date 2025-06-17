package com.example.babycareapp.models

import java.io.Serializable

data class Review(
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Float = 0f,
    val comment: String = ""
) : Serializable
