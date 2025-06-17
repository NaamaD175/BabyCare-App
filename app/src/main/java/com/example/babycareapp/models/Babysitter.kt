package com.example.babycareapp.models

import java.io.Serializable

data class Babysitter(
    val name: String = "",
    val address: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val uploaderId: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    var averageRating: Float = 0f,
    var numberOfRatings: Int = 0,
    var reviews: List<Review> = emptyList()
) : Serializable



