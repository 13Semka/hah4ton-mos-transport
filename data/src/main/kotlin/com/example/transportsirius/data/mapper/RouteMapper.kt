package com.example.transportsirius.data.mapper

import com.example.transportsirius.data.dto.LatLngDto
import com.example.transportsirius.data.dto.RouteOptionDto
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import javax.inject.Inject

/**
 * Класс для преобразования DTO в доменные модели и обратно
 */
class RouteMapper @Inject constructor() {
    
    fun mapToDomain(dto: RouteOptionDto): RouteOption {
        return RouteOption(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            duration = dto.duration,
            distance = dto.distance,
            price = dto.price,
            transportType = dto.transportType,
            points = dto.points.map { mapLatLngToDomain(it) }
        )
    }
    
    fun mapLatLngToDomain(dto: LatLngDto): LatLng {
        return LatLng(
            latitude = dto.latitude,
            longitude = dto.longitude
        )
    }
    
    fun mapLatLngToDto(entity: LatLng): LatLngDto {
        return LatLngDto(
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }

    fun mapToDto(domain: RouteOption): RouteOptionDto {
        return RouteOptionDto(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            duration = domain.duration,
            distance = domain.distance,
            price = domain.price,
            transportType = domain.transportType,
            points = domain.points.map { mapToDto(it) }
        )
    }

    private fun mapToDto(domain: LatLng): LatLngDto {
        return LatLngDto(
            latitude = domain.latitude,
            longitude = domain.longitude
        )
    }
}