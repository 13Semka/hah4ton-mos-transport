package com.example.transportsirius.data.api

import com.example.transportsirius.data.dto.GeocoderResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocoderApi {
    @GET("items/geocode")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("fields") fields: String = "items.point,items.address_name,items.full_name",
        @Query("key") apiKey: String
    ): GeocoderResponseDto

    @GET("items/geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("fields") fields: String = "items.point,items.address_name,items.full_name",
        @Query("key") apiKey: String
    ): GeocoderResponseDto
} 