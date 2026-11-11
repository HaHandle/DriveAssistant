package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            vehicleConnection = AutomotiveUxConnection(this) { vehicleUxState = it }
        } else {
            vehicleUxState = VehicleUxState.PREVIEW
        }
        setContent {
            DrivingCoachApp(vehicleUxState = vehicleUxState, foreground = foreground)
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