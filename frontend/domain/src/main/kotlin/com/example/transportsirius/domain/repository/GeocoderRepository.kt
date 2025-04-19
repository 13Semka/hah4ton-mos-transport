package com.example.transportsirius.domain.repository

import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
 
interface GeocoderRepository {
    suspend fun geocode(address: String): List<GeocoderResult>
    suspend fun reverseGeocode(latLng: LatLng): List<GeocoderResult>
    suspend fun getCurrentLocation(): LatLng
} 