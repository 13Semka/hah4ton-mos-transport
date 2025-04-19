package com.example.transportsirius.data.db.converters

import androidx.room.TypeConverter
import com.example.transportsirius.data.db.entity.LatLngEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Конвертеры для типов данных, которые Room не может сохранять напрямую
 */
class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromLatLngList(value: List<LatLngEntity>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toLatLngList(value: String): List<LatLngEntity> {
        val listType = object : TypeToken<List<LatLngEntity>>() {}.type
        return gson.fromJson(value, listType)
    }
} 