package com.example.transportsirius.domain.entity

data class GeocoderResult(
    val name: String,
    val formattedAddress: String,
    val latLng: LatLng
) 