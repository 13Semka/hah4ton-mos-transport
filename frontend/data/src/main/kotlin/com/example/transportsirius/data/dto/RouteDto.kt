package com.example.transportsirius.data.dto

import com.google.gson.annotations.SerializedName

data class LatLngDto(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lng") val longitude: Double
)

data class RouteOptionDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("distance") val distance: Double,
    @SerializedName("price") val price: Double,
    @SerializedName("type") val transportType: String,
    @SerializedName("points") val points: List<LatLngDto>
)

data class RoutesResponseDto(
    @SerializedName("routes") val routes: List<RouteOptionDto>
) 