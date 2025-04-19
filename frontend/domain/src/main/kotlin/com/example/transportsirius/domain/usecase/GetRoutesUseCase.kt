package com.example.transportsirius.domain.usecase

import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.domain.repository.RouteRepository

class GetRoutesUseCase(private val repo: RouteRepository) {
    suspend operator fun invoke(from: LatLng, to: LatLng): List<RouteOption> =

        repo.getRoutes(from, to)
}