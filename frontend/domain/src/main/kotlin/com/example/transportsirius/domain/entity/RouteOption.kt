package com.example.transportsirius.domain.entity

data class RouteOption(
    val id: String,
    val name: String,
    val description: String,
    val duration: Long,
    val distance: Double,
    val price: Double,
    val transportType: String,
    val points: List<LatLng>
)