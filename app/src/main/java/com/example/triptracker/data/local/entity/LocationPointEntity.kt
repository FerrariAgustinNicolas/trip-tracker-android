package com.example.triptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_points")
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tripId: Long,

    val latitude: Double,
    val longitude: Double,

    val accuracy: Float,
    val speed: Float? = null,

    val timestamp: Long
)