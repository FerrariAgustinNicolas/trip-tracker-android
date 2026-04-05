package com.example.triptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTime: Long,
    val endTime: Long? = null,

    val distanceKm: Double = 0.0,
    val pricePerKm: Double = 0.0,
    val totalAmount: Double = 0.0,

    val status: String,
    val notes: String? = null
)