package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivingcoach.data.createDriverOverviewRepository
import com.example.drivingcoach.ui.DriverOverviewViewModel
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.vehicle.AutomotiveUxConnection
import com.example.drivingcoach.vehicle.VehicleUxState

class MainActivity : ComponentActivity() {
    private var vehicleUxState by mutableStateOf(VehicleUxState.UNAVAILABLE)
    private var foreground by mutableStateOf(false)
    private var vehicleConnection: AutomotiveUxConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val automotive = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (automotive) {
            vehicleConnection = AutomotiveUxConnection(this) { vehicleUxState = it }
        } else {
            vehicleUxState = VehicleUxState.PREVIEW
        }
        val overviewViewModel = ViewModelProvider(
            this, DriverOverviewViewModel.Factory(createDriverOverviewRepository(automotive)),
        )[DriverOverviewViewModel::class.java]
        setContent {
            val overviewState by overviewViewModel.uiState.collectAsStateWithLifecycle()
            DrivingCoachApp(
                vehicleUxState = vehicleUxState,
                foreground = foreground,
                overviewState = overviewState,
                onRetryOverview = overviewViewModel::retry,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        vehicleConnection?.start()
    }

    override fun onStop() {
        foreground = false
        vehicleConnection?.stop()
        super.onStop()
    }
}
