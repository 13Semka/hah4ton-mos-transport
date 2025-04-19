package com.example.transportsirius.domain.usecase

import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.repository.GeocoderRepository
import javax.inject.Inject

class GeocodingUseCase @Inject constructor(
    private val geocoderRepository: GeocoderRepository
) {
    suspend fun geocode(address: String): List<GeocoderResult> {
        return geocoderRepository.geocode(address)
    }

    suspend fun reverseGeocode(latLng: LatLng): List<GeocoderResult> {
        return geocoderRepository.reverseGeocode(latLng)
    }

    suspend fun getCurrentLocation(): LatLng {
        return geocoderRepository.getCurrentLocation()
    }
} 