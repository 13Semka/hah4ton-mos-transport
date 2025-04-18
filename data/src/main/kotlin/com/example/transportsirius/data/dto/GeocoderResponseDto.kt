package com.example.transportsirius.data.dto

import com.google.gson.annotations.SerializedName

data class GeocoderResponseDto(
    @SerializedName("result") val result: ResultDto?
)

data class ResultDto(
    @SerializedName("items") val items: List<ItemDto>?
)

data class ItemDto(
    @SerializedName("address_name") val addressName: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("point") val point: PointDto?
)

data class PointDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
) 