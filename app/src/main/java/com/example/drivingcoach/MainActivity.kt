package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val automotive by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
    private var restrictionsMonitor: CarRestrictionsMonitor? = null
    private var vehicleRestricted by mutableStateOf<Boolean?>(null)

    private var speedSample by mutableStateOf<SpeedSample?>(null)
    private var speedPermissionGranted by mutableStateOf(false)
    private var interruptionVersion by mutableStateOf(0)
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        speedPermissionGranted = granted
        if (granted) restrictionsMonitor?.refreshSpeedSubscription()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrivingCoachUITheme {
                if (automotive) {
                    TrainingFlow(
                        drivingRestricted = vehicleRestricted != false,
                        restrictionKnown = vehicleRestricted != null,
                        demo = false,
                        speedSample = speedSample,
                        interruptionVersion = interruptionVersion,
                        demoControls = {
                            if (vehicleRestricted == false && !speedPermissionGranted) {
                                TextButton(onClick = { speedPermission.launch("android.car.permission.CAR_SPEED") }) {
                                    Text("속도 데이터 권한 허용")
                                }
                            }
                        }
                    )
                } else {
                    TrainingDemo()
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if (automotive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            speedPermissionGranted = checkSelfPermission("android.car.permission.CAR_SPEED") ==
                PackageManager.PERMISSION_GRANTED
            val monitor = CarRestrictionsMonitor(
                this,
                onRestrictionChanged = { vehicleRestricted = it },
                onSpeedChanged = { speedSample = it }
            )
            restrictionsMonitor = monitor
            monitor.start()
        }
    }

    override fun onStop() {
        interruptionVersion++
        restrictionsMonitor?.stop()
        restrictionsMonitor = null
        vehicleRestricted = null
        super.onStop()
    }
}

private enum class TrainingScreen {
    Home, Session, Result
}

private data class ScreenContent(
    val title: String,
    val description: String,
    val cardTitle: String,
    val cardText: String,
    val action: String
)

@Composable
private fun TrainingDemo() {
    var drivingRestricted by rememberSaveable { mutableStateOf(false) }
    TrainingFlow(
        drivingRestricted = drivingRestricted,
        demoControls = {
            TextButton(onClick = { drivingRestricted = !drivingRestricted }) {
                Text(if (drivingRestricted) "데모 · 주차 상태로 전환" else "데모 · 주행 제한 재현")
            }
        }
    )
}

@Composable
private fun TrainingFlow(
    initialScreen: TrainingScreen = TrainingScreen.Home,
    drivingRestricted: Boolean = false,
    restrictionKnown: Boolean = true,
    demo: Boolean = true,
    speedSample: SpeedSample? = null,
    interruptionVersion: Int = 0,
    demoControls: @Composable () -> Unit = {}
) {
    var screen by remember { mutableStateOf(initialScreen) }
    var scenario by rememberSaveable { mutableStateOf(DemoScenario.Smooth) }
    var session by remember { mutableStateOf(DecelerationSession()) }
    var replayFinished by remember { mutableStateOf(false) }
    var replaySpeed by remember { mutableStateOf<Float?>(null) }
    var result by remember { mutableStateOf<DecelerationResult?>(null) }

    LaunchedEffect(screen, session) {
        if (screen == TrainingScreen.Session && demo) {
            replayFinished = false
            for (index in scenario.speeds.indices) {
                val sample = scenario.sample(index)
                session.add(sample)
                replaySpeed = sample.metersPerSecond
                delay(1000)
            }
            replayFinished = true
        }
    }
    LaunchedEffect(speedSample, screen, session) {
        if (!demo && screen == TrainingScreen.Session) {
            val sample = speedSample
            if (sample == null) session.interrupt() else session.add(sample)
        }
    }
    LaunchedEffect(interruptionVersion) {
        if (!demo && screen == TrainingScreen.Session) session.interrupt()
    }
    BackHandler(enabled = !drivingRestricted && screen != TrainingScreen.Home) {
        screen = TrainingScreen.Home
    }

    val content = if (drivingRestricted) null else when (screen) {
        TrainingScreen.Home -> ScreenContent(
            title = "운전 연수",
            description = if (demo) "데모 속도 데이터를 재생해 감속을 평가해요." else "속도 데이터가 수신되면 연수를 시작할 수 있어요.",
            cardTitle = "기본 연수",
            cardText = "주차 상태에서 시작한 뒤 주차 상태로 돌아와 결과를 확인해 주세요.",
            action = "연수 시작"
        )
        TrainingScreen.Session -> ScreenContent(
            title = "연수 진행 중",
            description = if (demo) "시나리오 속도 재생 중" else "차량 속도 수집 중",
            cardTitle = "현재 속도",
            cardText = (if (demo) replaySpeed else speedSample?.metersPerSecond)?.let {
                String.format(Locale.getDefault(), "%.1f km/h", it * 3.6f)
            } ?: "속도 데이터 대기 중",
            action = if (demo && !replayFinished) "데이터 재생 중…" else "연수 종료 및 결과 보기"
        )
        TrainingScreen.Result -> ScreenContent(
            title = "연수 결과",
            description = if (demo) "데모 데이터 평가" else "차량 속도 데이터 평가",
            cardTitle = result?.score?.let { "실험용 감속 점수 ${it}점" } ?: "평가 보류",
            cardText = result?.let {
                it.feedback + if (it.score != null) {
                    String.format(Locale.getDefault(), "\n최대 감속 %.1f m/s²", it.peakDeceleration)
                } else ""
            } ?: "연수를 시작해 데이터를 수집해 주세요.",
            action = "홈으로"
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (demo) "DRIVE COACH · 데모" else "DRIVE COACH · 차량 연동",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (content == null) {
                    val title = if (restrictionKnown) "주행 중 이용 제한" else "차량 상태 확인 중"
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text("차량에서 이용 가능 상태가 확인되면 이어서 이용할 수 있어요.")
                } else {
                    if (demo && screen == TrainingScreen.Home) {
                        TextButton(onClick = {
                            scenario = if (scenario == DemoScenario.Smooth) DemoScenario.Abrupt else DemoScenario.Smooth
                        }) {
                            Text("시나리오 · " + scenario.label)
                        }
                    }
                    TrainingScreenContent(
                        content,
                        actionEnabled = when (screen) {
                            TrainingScreen.Home -> demo || speedSample != null
                            TrainingScreen.Session -> !demo || replayFinished
                            TrainingScreen.Result -> true
                        }
                    ) {
                        when (screen) {
                            TrainingScreen.Home -> {
                                session = DecelerationSession()
                                result = null
                                replaySpeed = null
                                replayFinished = false
                                screen = TrainingScreen.Session
                            }
                            TrainingScreen.Session -> {
                                result = session.finish(if (demo) null else SystemClock.elapsedRealtimeNanos())
                                screen = TrainingScreen.Result
                            }
                            TrainingScreen.Result -> screen = TrainingScreen.Home
                        }
                    }
                    if (screen == TrainingScreen.Result) {
                        Text("실험용 지표 · 감속 구간 중 3 m/s² 초과 구간의 시간 비율로 계산",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                demoControls()
            }
        }
    }
}

@Composable
private fun TrainingScreenContent(
    content: ScreenContent,
    actionEnabled: Boolean = true,
    onAction: () -> Unit
) {
    Text(content.title, style = MaterialTheme.typography.headlineLarge)
    Text(content.description, style = MaterialTheme.typography.bodyLarge)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(content.cardTitle, style = MaterialTheme.typography.titleMedium)
            Text(content.cardText, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Button(
        onClick = onAction,
        enabled = actionEnabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Text(content.action)
    }
}

@Preview(name = "연수 홈", widthDp = 840, heightDp = 480)
@Composable
private fun HomePreview() {
    DrivingCoachUITheme { TrainingFlow() }
}

@Preview(name = "연수 진행", widthDp = 840, heightDp = 480)
@Composable
private fun SessionPreview() {
    DrivingCoachUITheme { TrainingFlow(TrainingScreen.Session) }
}

@Preview(name = "연수 결과", widthDp = 840, heightDp = 480)
@Composable
private fun ResultPreview() {
    DrivingCoachUITheme { TrainingFlow(TrainingScreen.Result) }
}

@Preview(name = "주행 제한 데모", widthDp = 840, heightDp = 480)
@Composable
private fun DrivingRestrictionPreview() {
    DrivingCoachUITheme {
        TrainingFlow(initialScreen = TrainingScreen.Result, drivingRestricted = true)
    }
}
