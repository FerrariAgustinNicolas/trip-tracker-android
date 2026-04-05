package com.example.triptracker.util

import android.location.Location
import com.example.triptracker.data.local.entity.LocationPointEntity

object DistanceUtils {

    fun calculateDistanceKm(points: List<LocationPointEntity>): Double {
        if (points.size < 2) return 0.0

        var totalMeters = 0f

        for (i in 0 until points.lastIndex) {
            val current = points[i]
            val next = points[i + 1]

            val results = FloatArray(1)
            Location.distanceBetween(
                current.latitude,
                current.longitude,
                next.latitude,
                next.longitude,
                results
            )

            totalMeters += results[0]
        }

        return totalMeters / 1000.0
    }
}