package com.example.transportsirius.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.transportsirius.data.api.GeocoderApi
import com.example.transportsirius.data.mapper.GeocoderMapper
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.repository.GeocoderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderRepositoryImpl @Inject constructor(
    private val geocoderApi: GeocoderApi,
    private val geocoderMapper: GeocoderMapper,
    @ApplicationContext private val context: Context
) : GeocoderRepository {

    companion object {
        private const val TAG = "GeocoderRepo"
        private const val API_KEY = "c3b80e3a-2669-4cc5-950e-647604fda4c1" // Ключ API 2GIS
        // Дефолтные координаты (Москва, Красная площадь)
        private val DEFAULT_LOCATION = LatLng(55.751244, 37.618423)
    }

    override suspend fun geocode(address: String): List<GeocoderResult> {
        return try {
            val response = geocoderApi.geocode(
                query = address,
                apiKey = API_KEY
            )
            geocoderMapper.mapResponseToDomain(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding address: $address", e)
            emptyList()
        }
    }

    override suspend fun reverseGeocode(latLng: LatLng): List<GeocoderResult> {
        return try {
            val response = geocoderApi.reverseGeocode(
                lat = latLng.latitude,
                lon = latLng.longitude,
                apiKey = API_KEY
            )
            geocoderMapper.mapResponseToDomain(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error reverse geocoding location: $latLng", e)
            emptyList()
        }
    }

    override suspend fun getCurrentLocation(): LatLng = withContext(Dispatchers.IO) {
        try {
            // Проверяем наличие разрешений
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Нет разрешений, логируем и возвращаем дефолтные координаты
                Log.d(TAG, "Location permissions not granted, using default location")
                return@withContext DEFAULT_LOCATION
            }

            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                // Проверяем, включены ли провайдеры местоположения
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                Log.d(TAG, "GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")

                if (!isGpsEnabled && !isNetworkEnabled) {
                    Log.d(TAG, "No location providers enabled, using default location")
                    return@withContext DEFAULT_LOCATION
                }

                // Пробуем получить местоположение из разных провайдеров
                var bestLocation: Location? = null

                if (isGpsEnabled) {
                    try {
                        val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (gpsLocation != null) {
                            Log.d(TAG, "Got GPS location: ${gpsLocation.latitude}, ${gpsLocation.longitude}")
                            bestLocation = gpsLocation
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting GPS location", e)
                    }
                }

                if (isNetworkEnabled) {
                    try {
                        val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (networkLocation != null) {
                            Log.d(TAG, "Got Network location: ${networkLocation.latitude}, ${networkLocation.longitude}")
                            if (bestLocation == null || networkLocation.accuracy < bestLocation.accuracy) {
                                bestLocation = networkLocation
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting Network location", e)
                    }
                }

                // Проверяем все провайдеры
                val providers = locationManager.getProviders(true)
                Log.d(TAG, "Available providers: $providers")

                for (provider in providers) {
                    if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
                        // Уже обработали выше
                        continue
                    }

                    try {
                        val location = locationManager.getLastKnownLocation(provider) ?: continue
                        Log.d(TAG, "Got location from $provider: ${location.latitude}, ${location.longitude}")
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            bestLocation = location
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting location from $provider", e)
                    }
                }

                return@withContext if (bestLocation != null) {
                    Log.d(TAG, "Using best location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                    LatLng(bestLocation.latitude, bestLocation.longitude)
                } else {
                    Log.d(TAG, "No location available, using default location")
                    DEFAULT_LOCATION
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                return@withContext DEFAULT_LOCATION
            }
        } catch (e: Exception) {
            Log.e(TAG, "Global error in getCurrentLocation", e)
            DEFAULT_LOCATION
        }
    }
} 