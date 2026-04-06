package com.example.triptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.triptracker.data.local.database.DatabaseProvider
import com.example.triptracker.data.repository.TripRepository
import com.example.triptracker.location.LocationTracker
import com.example.triptracker.viewmodel.MainViewModel
import com.example.triptracker.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

class MainActivity : ComponentActivity() {

    private lateinit var locationTracker: LocationTracker

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            TripRepository(
                DatabaseProvider.getDatabase(applicationContext).tripDao()
            )
        )
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLocationTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationTracker = LocationTracker(applicationContext)

        observeLocationUpdates()
        observeTrackingState()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onStartTrip = { pricePerKm ->
                            viewModel.startTrip(pricePerKm)
                        },
                        onEndTrip = {
                            viewModel.endTrip()
                        }
                    )
                }
            }
        }
    }

    private fun observeLocationUpdates() {
        lifecycleScope.launch {
            locationTracker.locationUpdates.collect { location ->
                viewModel.onLocationUpdate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed
                )
            }
        }
    }

    private fun observeTrackingState() {
        lifecycleScope.launch {
            viewModel.isTracking.collect { tracking ->
                if (tracking) {
                    checkLocationPermissionAndStart()
                } else {
                    locationTracker.stopTracking()
                }
            }
        }
    }

    private fun checkLocationPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationTracking()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startLocationTracking() {
        locationTracker.startTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onStartTrip: (Double) -> Unit,
    onEndTrip: () -> Unit
) {
    val currentTrip by viewModel.currentTrip.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val pricePerKm by viewModel.pricePerKm.collectAsState()
    val trips by viewModel.trips.collectAsState(initial = emptyList())

    var priceInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Trip Tracker",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            OutlinedTextField(
                value = priceInput,
                onValueChange = { priceInput = it },
                label = { Text("Precio por km") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTracking
            )
        }

        item {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    if (price > 0) {
                        onStartTrip(price)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTracking
            ) {
                Text("Iniciar viaje")
            }
        }

        item {
            Button(
                onClick = onEndTrip,
                modifier = Modifier.fillMaxWidth(),
                enabled = isTracking
            ) {
                Text("Finalizar viaje")
            }
        }

        item {
            Text(
                text = if (currentTrip != null && isTracking) {
                    "Estado: viaje en curso"
                } else {
                    "Estado: sin viaje activo"
                }
            )
        }

        item {
            Text(text = "Distancia recorrida: %.2f km".format(distanceKm))
        }

        item {
            Text(text = "Precio por km: %.2f".format(pricePerKm))
        }

        item {
            Text(text = "Total recorrido: %.2f".format(viewModel.totalAmount))
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Historial de viajes",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (trips.isEmpty()) {
            item {
                Text("Todavía no hay viajes guardados.")
            }
        } else {
            items(
                items = trips,
                key = { trip -> trip.id }
            ) { trip: com.example.triptracker.data.local.entity.TripEntity ->
                TripItem(
                    date = trip.startTime,
                    distanceKm = trip.distanceKm,
                    pricePerKm = trip.pricePerKm,
                    totalAmount = trip.totalAmount,
                    status = trip.status
                )
            }
        }
    }
}


@Composable
fun TripItem(
    date: Long,
    distanceKm: Double,
    pricePerKm: Double,
    totalAmount: Double,
    status: String
) {
    val formattedDate = remember(date) {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(date))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Estado: $status")
            Text(text = "Distancia: %.2f km".format(distanceKm))
            Text(text = "Precio por km: %.2f".format(pricePerKm))
            Text(text = "Total: %.2f".format(totalAmount))
        }
    }
}