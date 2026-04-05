package com.example.triptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.triptracker.data.local.entity.LocationPointEntity
import com.example.triptracker.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Insert
    suspend fun insertLocationPoint(point: LocationPointEntity)

    @Insert
    suspend fun insertLocationPoints(points: List<LocationPointEntity>)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForTrip(tripId: Long): List<LocationPointEntity>

    @Query("SELECT * FROM trips WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getInProgressTrip(): TripEntity?
}