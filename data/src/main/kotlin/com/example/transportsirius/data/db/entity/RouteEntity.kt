package com.example.transportsirius.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.transportsirius.data.db.converters.Converters

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val duration: Long,
    val distance: Double,
    val price: Double,
    val transportType: String,
    @TypeConverters(Converters::class)
    val points: List<LatLngEntity>
)

data class LatLngEntity(
    val latitude: Double,
    val longitude: Double
) 