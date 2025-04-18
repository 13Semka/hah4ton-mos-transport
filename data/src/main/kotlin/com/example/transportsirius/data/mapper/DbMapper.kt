package com.example.transportsirius.data.mapper

import com.example.transportsirius.data.db.entity.LatLngEntity
import com.example.transportsirius.data.db.entity.RouteEntity
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import javax.inject.Inject

/**
 * Маппер для конвертации между сущностями базы данных и доменными моделями
 */
class DbMapper @Inject constructor() {
    
    fun mapToEntity(domain: RouteOption): RouteEntity {
        return RouteEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            duration = domain.duration,
            distance = domain.distance,
            price = domain.price,
            transportType = domain.transportType,
            points = domain.points.map { mapToEntity(it) }
        )
    }
    
    fun mapToDomain(entity: RouteEntity): RouteOption {
        return RouteOption(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            duration = entity.duration,
            distance = entity.distance,
            price = entity.price,
            transportType = entity.transportType,
            points = entity.points.map { mapToDomain(it) }
        )
    }
    
    private fun mapToEntity(domain: LatLng): LatLngEntity {
        return LatLngEntity(
            latitude = domain.latitude,
            longitude = domain.longitude
        )
    }
    
    private fun mapToDomain(entity: LatLngEntity): LatLng {
        return LatLng(
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }
} 