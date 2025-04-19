package com.example.transportsirius.domain.repository

import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption

interface RouteRepository {
    suspend fun getRoutes(from: LatLng, to: LatLng): List<RouteOption>
}