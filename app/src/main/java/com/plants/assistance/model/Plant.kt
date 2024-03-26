package com.plants.assistance.model

data class Plant(
    val id: Int,
    val common_name: String,
    val scientific_name: String,
    val bibliography: String,
    val image_url: String
)
