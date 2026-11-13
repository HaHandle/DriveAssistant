package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.drivingcoach.vehicle.VehicleSpeedConnection
import com.example.drivingcoach.analysis.DecelerationSession
import com.example.drivingcoach.analysis.SpeedSample
import com.example.drivingcoach.ui.TrainingResultDisplay
import com.example.drivingcoach.ui.TrainingUiState
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var vehicleUxState by mutableStateOf(VehicleUxState.UNAVAILABLE)
    private var foreground by mutableStateOf(false)
    private var vehicleConnection: AutomotiveUxConnection? = null
    private var speedConnection: VehicleSpeedConnection? = null
    private var speedSample: SpeedSample? = null
    private var trainingSession: DecelerationSession? = null
    private var trainingState by mutableStateOf(TrainingUiState())
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        trainingState = trainingState.copy(permissionGranted = granted)
        if (granted) speedConnection?.subscribe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val automotive = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (automotive) {
            vehicleConnection = AutomotiveUxConnection(this) { vehicleUxState = it }
            speedConnection = VehicleSpeedConnection(this) { sample ->
                if (sample == null) trainingSession?.interrupt() else trainingSession?.add(sample)
                speedSample = sample
                trainingState = trainingState.copy(speedKmh = sample?.metersPerSecond?.times(3.6))
            }
        } else {
            vehicleUxState = VehicleUxState.PREVIEW
        }
        trainingState = trainingState.copy(automotive = automotive)
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
                trainingState = trainingState,
                onRequestSpeedPermission = {
                    if (automotive && vehicleUxState == VehicleUxState.AVAILABLE) {
                        speedPermission.launch("android.car.permission.CAR_SPEED")
                    }
                },
                onStartTraining = ::startTraining,
                onFinishTraining = ::finishTraining,
                onResetTraining = { trainingSession = null; trainingState = trainingState.copy(active = false, result = null) },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        vehicleConnection?.start()
        if (speedConnection != null) {
            trainingState = trainingState.copy(
                permissionGranted = checkSelfPermission("android.car.permission.CAR_SPEED") ==
                    PackageManager.PERMISSION_GRANTED,
            )
            speedConnection?.start()
        }
    }

    override fun onStop() {
        foreground = false
        trainingSession?.interrupt()
        speedConnection?.stop()
        vehicleConnection?.stop()
        super.onStop()
    }

    private fun startTraining(scenario: Int) {
        if (vehicleUxState == VehicleUxState.PREVIEW) {
            val demo = com.example.drivingcoach.analysis.DemoScenario.entries
                .getOrElse(scenario) { com.example.drivingcoach.analysis.DemoScenario.Smooth }
            val session = DecelerationSession()
            demo.speeds.indices.forEach { session.add(demo.sample(it)) }
            trainingState = trainingState.copy(active = false, result = session.finish().toDisplay())
            return
        }
        val sample = speedSample
        val fresh = sample != null &&
            SystemClock.elapsedRealtimeNanos() - sample.timestampNanos in 0L..2_000_000_000L
        if (vehicleUxState != VehicleUxState.AVAILABLE ||
            !trainingState.permissionGranted || !fresh) return
        trainingSession = DecelerationSession(startedAtNanos = SystemClock.elapsedRealtimeNanos())
        trainingState = trainingState.copy(active = true, result = null)
    }

    private fun finishTraining() {
        if (vehicleUxState != VehicleUxState.AVAILABLE) return
        val session = trainingSession ?: return
        trainingSession = null
        trainingState = trainingState.copy(
            active = false,
            result = session.finish(SystemClock.elapsedRealtimeNanos(), includeDemoScore = false).toDisplay(),
        )
    }

    private fun com.example.drivingcoach.analysis.DecelerationResult.toDisplay(): TrainingResultDisplay {
        val stops = stopObservations.orEmpty().joinToString("\n") {
            val count = it.observedStops
            val detail = if (count >= 3) {
                "강한 감속 후보 미감지 ${it.stopsWithoutStrongCandidate}/${count}회"
            } else "추가 관찰 필요"
            "${it.speedBand.label}: ${count}회 · $detail"
        }
        return TrainingResultDisplay(
            feedback = feedback,
            score = score,
            strongEvents = strongEventCount,
            peakDeceleration = if (strongEventCount != null) {
                String.format(Locale.getDefault(), "%.1f m/s²", peakDeceleration)
            } else null,
            stopSummary = stops,
        )
    }
}
