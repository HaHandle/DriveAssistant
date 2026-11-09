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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
    private var vehicleSession: DecelerationSession? = null
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
                        startVehicleSession = ::startVehicleSession,
                        finishVehicleSession = ::finishVehicleSession,
                        cancelVehicleSession = { vehicleSession = null },
                        demoControls = {
                            if (vehicleRestricted == false && !speedPermissionGranted) {
                                SecondaryAction("속도 데이터 권한 허용") { speedPermission.launch("android.car.permission.CAR_SPEED") }
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
                onRestrictionChanged = { restricted ->
                    vehicleRestricted = restricted
                    if (restricted == null) vehicleSession?.interrupt()
                },
                onSpeedChanged = { sample ->
                    // Feed the recorder before Compose renders the latest speed.
                    if (sample == null) vehicleSession?.interrupt() else vehicleSession?.add(sample)
                    speedSample = sample
                }
            )
            restrictionsMonitor = monitor
            monitor.start()
        }
    }

    override fun onStop() {
        vehicleSession?.interrupt()
        restrictionsMonitor?.stop()
        restrictionsMonitor = null
        vehicleRestricted = null
        super.onStop()
    }

    private fun startVehicleSession(): Boolean {
        if (vehicleRestricted != false || !speedPermissionGranted || speedSample == null) return false
        vehicleSession = DecelerationSession(startedAtNanos = SystemClock.elapsedRealtimeNanos())
        return true
    }

    private fun finishVehicleSession(): DecelerationResult? {
        if (vehicleRestricted != false) return null
        val activeSession = vehicleSession ?: return null
        vehicleSession = null
        return activeSession.finish(SystemClock.elapsedRealtimeNanos(), includeDemoScore = false)
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
            SecondaryAction(if (drivingRestricted) "데모 · 주차 상태로 전환" else "데모 · 주행 제한 재현") {
                drivingRestricted = !drivingRestricted
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
    startVehicleSession: () -> Boolean = { false },
    finishVehicleSession: () -> DecelerationResult? = { null },
    cancelVehicleSession: () -> Unit = {},
    demoControls: @Composable () -> Unit = {}
) {
    var screen by remember { mutableStateOf(initialScreen) }
    var scenario by rememberSaveable { mutableStateOf(DemoScenario.Smooth) }
    var demoSession by remember { mutableStateOf(DecelerationSession()) }
    var replayFinished by remember { mutableStateOf(false) }
    var replaySpeed by remember { mutableStateOf<Float?>(null) }
    var result by remember { mutableStateOf<DecelerationResult?>(null) }

    LaunchedEffect(screen, demoSession) {
        if (screen == TrainingScreen.Session && demo) {
            replayFinished = false
            for (index in scenario.speeds.indices) {
                val sample = scenario.sample(index)
                demoSession.add(sample)
                replaySpeed = sample.metersPerSecond
                delay(1000)
            }
            replayFinished = true
        }
    }
    BackHandler(enabled = !drivingRestricted && screen != TrainingScreen.Home) {
        if (!demo && screen == TrainingScreen.Session) cancelVehicleSession()
        screen = TrainingScreen.Home
    }

    val content = if (drivingRestricted) null else when (screen) {
        TrainingScreen.Home -> ScreenContent(
            title = "운전 연수",
            description = if (demo) "속도 시나리오로 감속 연수를 체험하세요." else "속도 연결을 확인한 뒤 시작하세요.",
            cardTitle = "기본 연수",
            cardText = "주차 상태에서 시작하고, 연수 후 다시 주차해 결과를 확인하세요.",
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
            description = if (demo) "데모 데이터 평가" else "차량 속도 기반 관찰",
            cardTitle = result?.score?.let { "실험용 감속 점수 ${it}점" }
                ?: if (result?.strongEventCount != null) "감속 관찰" else "평가 보류",
            cardText = result?.let {
                val eventCount = it.strongEventCount
                if (eventCount == null) it.feedback else String.format(
                    Locale.getDefault(),
                    "강한 감속 후보 %d회\n구간 최대 감속 %.1f m/s²\n%s",
                    eventCount, it.peakDeceleration, it.feedback
                )
            } ?: "연수를 시작해 데이터를 수집해 주세요.",
            action = "홈으로"
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            // Use the application window, not full display dimensions or a simulated system GNB.
            val wide = maxWidth >= 840.dp
            val inset = if (maxWidth >= 1000.dp) 40.dp else 24.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(inset),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    if (demo) "DRIVE COACH · 데모" else "DRIVE COACH · 차량 연동",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (content == null) {
                    // No score, speed, action or detailed feedback while restricted/unknown.
                    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(if (restrictionKnown) "주행 중 이용 제한" else "차량 상태 확인 중",
                                style = MaterialTheme.typography.titleLarge)
                            Text("이용 가능한 차량 상태가 확인되면 화면이 다시 열려요.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    TrainingScreenContent(content, wide,
                        actionEnabled = when (screen) {
                            TrainingScreen.Home -> demo || speedSample != null
                            TrainingScreen.Session -> !demo || replayFinished
                            TrainingScreen.Result -> true
                        },
                        options = {
                            if (demo && screen == TrainingScreen.Home) {
                                SecondaryAction("시나리오 · " + scenario.label) {
                                    scenario = if (scenario == DemoScenario.Smooth) DemoScenario.Abrupt else DemoScenario.Smooth
                                }
                            }
                        }
                    ) {
                        when (screen) {
                            TrainingScreen.Home -> {
                                if (demo) {
                                    demoSession = DecelerationSession()
                                    replaySpeed = null
                                    replayFinished = false
                                } else if (!startVehicleSession()) {
                                    return@TrainingScreenContent
                                }
                                result = null
                                screen = TrainingScreen.Session
                            }
                            TrainingScreen.Session -> {
                                val completed = if (demo) demoSession.finish() else finishVehicleSession()
                                result = completed ?: return@TrainingScreenContent
                                screen = TrainingScreen.Result
                            }
                            TrainingScreen.Result -> screen = TrainingScreen.Home
                        }
                    }
                    if (screen == TrainingScreen.Result && result?.score != null) {
                        Text("데모 점수 · 감속 중 3 m/s² 초과 시간 비율로 계산",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Demo-only simulation switch is absent from the actual vehicle app.
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { demoControls() }
            }
        }
    }
}

@Composable
private fun TrainingScreenContent(
    content: ScreenContent,
    wide: Boolean,
    actionEnabled: Boolean = true,
    options: @Composable () -> Unit = {},
    onAction: () -> Unit
) {
    val heading: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(content.title, style = MaterialTheme.typography.titleLarge)
            Text(content.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            options()
        }
    }
    val detail: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(content.cardTitle, style = MaterialTheme.typography.titleSmall)
                    content.cardText.split("\n").forEach { paragraph ->
                        val sentences = paragraph.split(". ")
                        sentences.forEachIndexed { index, sentence ->
                            Text(sentence + if (index < sentences.lastIndex) "." else "",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            PrimaryAction(content.action, actionEnabled, onAction)
        }
    }
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(0.42f)) { heading() }
            Box(Modifier.weight(0.58f)) { detail() }
        }
    } else {
        heading()
        detail()
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    // Basic600 enabled / Basic700 light pressed / Basic500 dark pressed.
    val isDark = colors.background == com.example.drivingcoach.ui.theme.Basic50Dark
    val pressedColor = if (isDark) com.example.drivingcoach.ui.theme.Basic500Dark
        else androidx.compose.ui.graphics.Color(0xFF313236)
    Button(onClick = onClick, enabled = enabled, interactionSource = interactions,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) pressedColor else colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.onSurfaceVariant
        )) {
        // In-progress label stays visible when disabled; no distracting animated spinner.
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
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

@Preview(name = "분할 창 · 세로", widthDp = 600, heightDp = 960)
@Composable
private fun PortraitPreview() { DrivingCoachUITheme { TrainingFlow() } }

@Preview(name = "가로 창 · 주간", widthDp = 1280, heightDp = 720)
@Composable
private fun WidePreview() { DrivingCoachUITheme(darkTheme = false) { TrainingFlow() } }

@Preview(name = "가로 창 · 야간", widthDp = 1280, heightDp = 720)
@Composable
private fun NightPreview() { DrivingCoachUITheme(darkTheme = true) { TrainingFlow() } }

@Preview(name = "큰 글자 · 분할 창", widthDp = 600, heightDp = 960, fontScale = 1.3f)
@Composable
private fun LargeTextPreview() { DrivingCoachUITheme { TrainingFlow() } }
