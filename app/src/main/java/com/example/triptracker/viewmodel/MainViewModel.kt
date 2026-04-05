package com.example.triptracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triptracker.data.local.entity.LocationPointEntity
import com.example.triptracker.data.local.entity.TripEntity
import com.example.triptracker.data.repository.TripRepository
import com.example.triptracker.util.DistanceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TripRepository
) : ViewModel() {

    private val _currentTrip = MutableStateFlow<TripEntity?>(null)
    val currentTrip: StateFlow<TripEntity?> = _currentTrip.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _pricePerKm = MutableStateFlow(0.0)
    val pricePerKm: StateFlow<Double> = _pricePerKm.asStateFlow()

    private var lastAcceptedLocation: LocationPointEntity? = null

    init {
        loadInProgressTrip()
    }

    private fun loadInProgressTrip() {
        viewModelScope.launch {
            val trip = repository.getInProgressTrip()
            _currentTrip.value = trip
            _isTracking.value = trip != null

            if (trip != null) {
                _pricePerKm.value = trip.pricePerKm
                recalculateDistance(trip.id)
            }
        }
    }

    fun startTrip(pricePerKm: Double) {
        viewModelScope.launch {
            val tripId = repository.startTrip(pricePerKm)
            val trip = repository.getTripById(tripId)
            lastAcceptedLocation = null
            _currentTrip.value = trip
            _pricePerKm.value = pricePerKm
            _distanceKm.value = 0.0
            _isTracking.value = true
        }
    }

    fun addLocationPoint(
        tripId: Long,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float? = null
    ) {
        viewModelScope.launch {
            val point = LocationPointEntity(
                tripId = tripId,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                speed = speed,
                timestamp = System.currentTimeMillis()
            )

            repository.insertLocationPoint(point)
            recalculateDistance(tripId)
        }
    }

    fun onLocationUpdate(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float? = null
    ) {
        val trip = _currentTrip.value ?: return
        if (!_isTracking.value) return

        if (accuracy > 25f) return

        val newPoint = LocationPointEntity(
            tripId = trip.id,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speed = speed,
            timestamp = System.currentTimeMillis()
        )

        val previousPoint = lastAcceptedLocation
        if (previousPoint != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                previousPoint.latitude,
                previousPoint.longitude,
                newPoint.latitude,
                newPoint.longitude,
                results
            )

            val distanceMeters = results[0]

            if (distanceMeters < 10f) {
                return
            }
        }

        addLocationPoint(
            tripId = trip.id,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speed = speed
        )

        lastAcceptedLocation = newPoint
    }
    fun endTrip() {
        viewModelScope.launch {
            val trip = _currentTrip.value ?: return@launch
            val points = repository.getLocationPointsForTrip(trip.id)
            val distance = DistanceUtils.calculateDistanceKm(points)

            val updatedTrip = repository.endTrip(trip, distance)

            lastAcceptedLocation = null
            _currentTrip.value = updatedTrip
            _distanceKm.value = updatedTrip.distanceKm
            _isTracking.value = false
        }
    }

    private suspend fun recalculateDistance(tripId: Long) {
        val points = repository.getLocationPointsForTrip(tripId)
        _distanceKm.value = DistanceUtils.calculateDistanceKm(points)
    }

    val totalAmount: Double
        get() = _distanceKm.value * _pricePerKm.value
}

