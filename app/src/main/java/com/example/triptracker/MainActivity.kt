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

    var priceInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Trip Tracker",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = priceInput,
            onValueChange = { priceInput = it },
            label = { Text("Precio por km") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTracking
        )

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

        Button(
            onClick = onEndTrip,
            modifier = Modifier.fillMaxWidth(),
            enabled = isTracking
        ) {
            Text("Finalizar viaje")
        }

        Text(
            text = if (currentTrip != null && isTracking) {
                "Estado: viaje en curso"
            } else {
                "Estado: sin viaje activo"
            }
        )

        Text(text = "Distancia recorrida: %.2f km".format(distanceKm))
        Text(text = "Precio por km: %.2f".format(pricePerKm))
        Text(text = "Total recorrido: %.2f".format(viewModel.totalAmount))

    }
}