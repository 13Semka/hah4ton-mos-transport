package com.example.transportsirius.data.api

import com.example.transportsirius.data.dto.RoutesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteApi {
    
    @GET("routes")
    suspend fun getRoutes(
        @Query("fromLat") fromLat: Double,
        @Query("fromLng") fromLng: Double,
        @Query("toLat") toLat: Double,
        @Query("toLng") toLng: Double
    ): RoutesResponseDto
} 