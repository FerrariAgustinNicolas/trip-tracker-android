package com.example.triptracker.data.repository

import com.example.triptracker.data.local.dao.TripDao
import com.example.triptracker.data.local.entity.LocationPointEntity
import com.example.triptracker.data.local.entity.TripEntity
import com.example.triptracker.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao
) {

    suspend fun startTrip(pricePerKm: Double): Long {
        val trip = TripEntity(
            startTime = System.currentTimeMillis(),
            pricePerKm = pricePerKm,
            status = TripStatus.IN_PROGRESS.name
        )
        return tripDao.insertTrip(trip)
    }

    suspend fun endTrip(trip: TripEntity, distanceKm: Double): TripEntity {
        val updatedTrip = trip.copy(
            endTime = System.currentTimeMillis(),
            distanceKm = distanceKm,
            totalAmount = distanceKm * trip.pricePerKm,
            status = TripStatus.FINISHED.name
        )
        tripDao.updateTrip(updatedTrip)
        return updatedTrip
    }

    suspend fun insertLocationPoint(point: LocationPointEntity) {
        tripDao.insertLocationPoint(point)
    }

    fun getAllTrips(): Flow<List<TripEntity>> {
        return tripDao.getAllTrips()
    }

    suspend fun getInProgressTrip(): TripEntity? {
        return tripDao.getInProgressTrip()
    }

    suspend fun getLocationPointsForTrip(tripId: Long): List<LocationPointEntity> {
        return tripDao.getLocationPointsForTrip(tripId)
    }
}