package com.example.transportsirius.data.repository

import com.example.transportsirius.data.api.RouteApi
import com.example.transportsirius.data.db.dao.RouteDao
import com.example.transportsirius.data.mapper.DbMapper
import com.example.transportsirius.data.mapper.RouteMapper
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.domain.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val api: RouteApi,
    private val routeDao: RouteDao,
    private val routeMapper: RouteMapper,
    private val dbMapper: DbMapper
) : RouteRepository {
    
    override suspend fun getRoutes(from: LatLng, to: LatLng): List<RouteOption> = 
        withContext(Dispatchers.IO) {
            try {
                // Пытаемся получить данные из API
                val response = api.getRoutes(
                    fromLat = from.latitude,
                    fromLng = from.longitude,
                    toLat = to.latitude,
                    toLng = to.longitude
                )
                
                val routes = response.routes.map { routeMapper.mapToDomain(it) }
                
                // Кэшируем маршруты в локальной базе данных
                routeDao.insertRoutes(routes.map { dbMapper.mapToEntity(it) })
                
                routes
            } catch (e: Exception) {
                // В случае ошибки получаем кэшированные данные из базы данных
                val cachedRoutes = routeDao.getAllRoutes().first()
                cachedRoutes.map { dbMapper.mapToDomain(it) }
            }
        }
    
    // Получение списка всех сохраненных маршрутов
    fun getAllSavedRoutes(): Flow<List<RouteOption>> {
        return routeDao.getAllRoutes().map { entities ->
            entities.map { dbMapper.mapToDomain(it) }
        }
    }
    
    // Получение маршрута по идентификатору
    suspend fun getRouteById(routeId: String): RouteOption? {
        return withContext(Dispatchers.IO) {
            routeDao.getRouteById(routeId)?.let { dbMapper.mapToDomain(it) }
        }
    }
}