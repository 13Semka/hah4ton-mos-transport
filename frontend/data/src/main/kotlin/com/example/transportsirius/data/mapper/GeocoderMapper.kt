package com.example.transportsirius.data.mapper

import com.example.transportsirius.data.dto.GeocoderResponseDto
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
import javax.inject.Inject

class GeocoderMapper @Inject constructor() {
    
    fun mapResponseToDomain(response: GeocoderResponseDto): List<GeocoderResult> {
        val resultItems = response.result?.items ?: emptyList()
        
        return resultItems.mapNotNull { item ->
            if (item.point != null) {
                GeocoderResult(
                    name = item.addressName ?: "",
                    formattedAddress = item.fullName ?: "",
                    latLng = LatLng(
                        latitude = item.point.lat,
                        longitude = item.point.lon
                    )
                )
            } else {
                null
            }
        }
    }
} 