package com.example.triptracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.triptracker.data.local.dao.TripDao
import com.example.triptracker.data.local.entity.LocationPointEntity
import com.example.triptracker.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}