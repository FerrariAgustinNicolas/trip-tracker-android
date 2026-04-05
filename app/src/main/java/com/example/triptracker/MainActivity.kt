package com.example.triptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.example.triptracker.viewmodel.MainViewModel
import com.example.triptracker.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            TripRepository(
                DatabaseProvider.getDatabase(applicationContext).tripDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
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
                    viewModel.startTrip(price)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTracking
        ) {
            Text("Iniciar viaje")
        }

        Button(
            onClick = { viewModel.endTrip() },
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

        Text(text = "Distancia acumulada: %.2f km".format(distanceKm))
        Text(text = "Precio por km: %.2f".format(pricePerKm))
        Text(text = "Total acumulado: %.2f".format(viewModel.totalAmount))
    }
}