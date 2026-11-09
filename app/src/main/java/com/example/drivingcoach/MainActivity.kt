package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val automotive by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
    private val practiceHistory by lazy { PracticeHistoryStore(applicationContext) }
    private var restrictionsMonitor: CarRestrictionsMonitor? = null
    private var vehicleRestricted by mutableStateOf<Boolean?>(null)
    private var vehicleParked by mutableStateOf<Boolean?>(null)
    private var parkedPracticeKey by mutableStateOf<Long?>(null)

    private var speedSample by mutableStateOf<SpeedSample?>(null)
    private var speedPermissionGranted by mutableStateOf(false)
    private var vehicleSession: DecelerationSession? = null
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        speedPermissionGranted = granted
        if (granted) restrictionsMonitor?.refreshSpeedSubscription()
    }

    private val canReadPark: Boolean
        get() = checkSelfPermission(PARK_SIGNAL_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private val canOpenParkedPractice: Boolean
        get() = canReadPark && vehicleRestricted == false && vehicleParked == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrivingCoachUITheme {
                if (automotive) {
                    VehicleContent()
                } else {
                    TrainingDemo(historyStore = practiceHistory)
                }
            }
        }
    }

    @Composable
    private fun VehicleContent() {
        val parkedRun = parkedPracticeKey
        if (parkedRun != null && canOpenParkedPractice) {
            key(parkedRun) {
                TrainingDemo(onExitParkedPractice = { parkedPracticeKey = null })
            }
            return
        }

        val sample = speedSample
        var fresh by remember(sample) { mutableStateOf((sample?.remainingFreshMillis() ?: 0L) > 0L) }
        LaunchedEffect(sample) {
            val remaining = sample?.remainingFreshMillis() ?: 0L
            if (remaining > 0L) delay(remaining)
            fresh = false
        }
        val availableSpeed = sample?.takeIf { fresh && speedPermissionGranted }
        val description = when {
            !speedPermissionGranted -> "차량 속도 읽기 권한이 필요해요."
            availableSpeed == null -> "차량 속도 신호를 기다리고 있어요."
            else -> "차량 속도가 연결됐어요."
        }
        TrainingFlow(
            drivingRestricted = vehicleRestricted != false,
            restrictionKnown = vehicleRestricted != null,
            demo = false,
            speedSample = availableSpeed,
            vehicleStatus = description,
            startVehicleSession = ::startVehicleSession,
            finishVehicleSession = ::finishVehicleSession,
            cancelVehicleSession = { vehicleSession = null },
            vehicleOptions = {
                if (canOpenParkedPractice) {
                    SecondaryAction("주차 중 모의 연수") {
                        if (canOpenParkedPractice) parkedPracticeKey = SystemClock.elapsedRealtimeNanos()
                    }
                } else if (canReadPark) {
                    val parkMessage = if (vehicleParked == false) "기어를 P에 두면 모의 연수를 열 수 있어요."
                    else "차량에서 주차 상태를 확인하는 중이에요."
                    Text(parkMessage, style = MaterialTheme.typography.bodySmall)
                }
                if (!speedPermissionGranted) {
                    SecondaryAction("속도 데이터 권한 허용") {
                        if (vehicleRestricted == false) speedPermission.launch(SPEED_PERMISSION)
                    }
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        if (automotive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            speedPermissionGranted = checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED
            val monitor = CarRestrictionsMonitor(
                this,
                onRestrictionChanged = { restricted ->
                    vehicleRestricted = restricted
                    if (restricted != false) parkedPracticeKey = null
                    if (restricted == null) vehicleSession?.interrupt()
                },
                onSpeedChanged = ::acceptVehicleSpeed,
                onParkChanged = { parked ->
                    vehicleParked = parked
                    if (parked != true) parkedPracticeKey = null
                }
            )
            restrictionsMonitor = monitor
            monitor.start()
        }
    }

    override fun onStop() {
        parkedPracticeKey = null
        vehicleSession?.interrupt()
        restrictionsMonitor?.stop()
        restrictionsMonitor = null
        vehicleRestricted = null
        vehicleParked = null
        speedSample = null
        super.onStop()
    }

    private fun acceptVehicleSpeed(sample: SpeedSample?) {
        val previous = speedSample
        if (sample != null && previous != null && sample.timestampNanos <= previous.timestampNanos) return
        if (sample == null) vehicleSession?.interrupt() else vehicleSession?.add(sample)
        speedSample = sample
    }

    private fun startVehicleSession(): Boolean {
        val remaining = speedSample?.remainingFreshMillis() ?: 0L
        if (vehicleRestricted != false || !speedPermissionGranted || remaining <= 0L) return false
        vehicleSession = DecelerationSession(startedAtNanos = SystemClock.elapsedRealtimeNanos())
        return true
    }

    private fun finishVehicleSession(): DecelerationResult? {
        if (vehicleRestricted != false) return null
        val activeSession = vehicleSession ?: return null
        vehicleSession = null
        return activeSession.finish(SystemClock.elapsedRealtimeNanos(), includeDemoScore = false)
    }

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
    }
}

private fun SpeedSample.remainingFreshMillis(): Long {
    val age = SystemClock.elapsedRealtimeNanos() - timestampNanos
    if (age !in 0L until MAX_SPEED_GAP_NANOS) return 0L
    return (MAX_SPEED_GAP_NANOS - age + 999_999L) / 1_000_000L
}
