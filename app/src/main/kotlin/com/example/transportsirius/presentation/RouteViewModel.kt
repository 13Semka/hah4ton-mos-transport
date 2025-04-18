package com.example.transportsirius.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.domain.repository.RouteRepository
import com.example.transportsirius.domain.usecase.GeocodingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val geocodingUseCase: GeocodingUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "RouteViewModel"
    }

    private val _fromLocation = MutableStateFlow<LatLng?>(null)
    val fromLocation: StateFlow<LatLng?> = _fromLocation

    private val _toLocation = MutableStateFlow<LatLng?>(null)
    val toLocation: StateFlow<LatLng?> = _toLocation
    
    private val _fromAddress = MutableStateFlow("")
    val fromAddress: StateFlow<String> = _fromAddress
    
    private val _toAddress = MutableStateFlow("")
    val toAddress: StateFlow<String> = _toAddress
    
    private val _searchResults = MutableLiveData<List<GeocoderResult>>()
    val searchResults: LiveData<List<GeocoderResult>> = _searchResults
    
    private val _routes = MutableLiveData<List<RouteOption>>()
    val routes: LiveData<List<RouteOption>> = _routes
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getCurrentLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Getting current location from usecase")
                val location = geocodingUseCase.getCurrentLocation()
                Log.d(TAG, "Got location: $location")
                _fromLocation.value = location
                
                Log.d(TAG, "Reverse geocoding location")
                val results = geocodingUseCase.reverseGeocode(location)
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Got address: ${results[0].formattedAddress}")
                    _fromAddress.value = results[0].formattedAddress
                } else {
                    Log.d(TAG, "No addresses found for location")
                    _fromAddress.value = "${location.latitude}, ${location.longitude}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current location", e)
                _error.value = "Ошибка определения местоположения: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchAddress(query: String, isFromAddress: Boolean) {
        if (query.length < 3) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Searching for address: $query")
                val results = geocodingUseCase.geocode(query)
                _searchResults.value = results
                Log.d(TAG, "Found ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching for address", e)
                _error.value = "Ошибка поиска адреса: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectSearchResult(result: GeocoderResult, isFromAddress: Boolean) {
        if (isFromAddress) {
            Log.d(TAG, "Selected from location: ${result.latLng}")
            _fromLocation.value = result.latLng
            _fromAddress.value = result.formattedAddress
        } else {
            Log.d(TAG, "Selected to location: ${result.latLng}")
            _toLocation.value = result.latLng
            _toAddress.value = result.formattedAddress
        }
        _searchResults.value = emptyList()
    }

    // Установка адреса отправления
    fun setFromAddress(result: GeocoderResult) {
        Log.d(TAG, "Setting from address: ${result.name}, location: ${result.latLng}")
        _fromLocation.value = result.latLng
        _fromAddress.value = result.formattedAddress
        _searchResults.value = emptyList()
    }
    
    // Установка адреса назначения
    fun setToAddress(result: GeocoderResult) {
        Log.d(TAG, "Setting to address: ${result.name}, location: ${result.latLng}")
        _toLocation.value = result.latLng
        _toAddress.value = result.formattedAddress
        _searchResults.value = emptyList()
    }

    fun loadRoutes() {
        val from = fromLocation.value
        val to = toLocation.value
        
        if (from != null && to != null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    Log.d(TAG, "Loading routes from $from to $to")
                    val routeOptions = routeRepository.getRoutes(from, to)
                    _routes.value = routeOptions
                    Log.d(TAG, "Found ${routeOptions.size} routes")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading routes", e)
                    _error.value = "Ошибка загрузки маршрутов: ${e.message}"
                    _routes.value = emptyList()
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _error.value = "Необходимо указать адреса отправления и назначения"
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}