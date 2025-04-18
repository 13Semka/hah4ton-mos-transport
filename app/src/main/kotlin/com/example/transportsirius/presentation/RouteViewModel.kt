package com.example.transportsirius.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    fun loadRoutes(from: LatLng, to: LatLng) = liveData<List<RouteOption>> {
        emit(routeRepository.getRoutes(from, to))
    }
}