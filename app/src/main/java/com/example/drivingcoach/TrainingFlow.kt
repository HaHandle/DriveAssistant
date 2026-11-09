package com.example.drivingcoach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal enum class TrainingScreen {
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
internal fun TrainingDemo(
    historyStore: PracticeHistoryStore? = null,
    onExitParkedPractice: (() -> Unit)? = null,
    replayActive: Boolean = true
) {
    val history = rememberPracticeHistory(historyStore)
    var drivingRestricted by rememberSaveable { mutableStateOf(false) }
    val phoneDemo = onExitParkedPractice == null
    BackHandler(enabled = !phoneDemo) { onExitParkedPractice?.invoke() }
    TrainingFlow(
        drivingRestricted = phoneDemo && drivingRestricted,
        practiceHistory = history.state,
        replayActive = replayActive,
        savesPracticeOnDevice = historyStore != null,
        onPracticeCompleted = history::record,
        onClearHistory = history::clear,
        onExitParkedPractice = onExitParkedPractice,
        demoControls = {
            if (phoneDemo) {
                SecondaryAction(if (drivingRestricted) "데모 · 주차 상태로 전환" else "데모 · 주행 제한 재현") {
                    drivingRestricted = !drivingRestricted
                }
            }
        }
    )
}

@Composable
internal fun TrainingFlow(
    initialScreen: TrainingScreen = TrainingScreen.Home,
    drivingRestricted: Boolean = false,
    restrictionKnown: Boolean = true,
    demo: Boolean = true,
    speedSample: SpeedSample? = null,
    vehicleStatus: String = "속도 연결을 확인한 뒤 시작하세요.",
    startVehicleSession: () -> Boolean = { false },
    finishVehicleSession: () -> DecelerationResult? = { null },
    cancelVehicleSession: () -> Unit = {},
    vehicleOptions: @Composable () -> Unit = {},
    practiceHistory: PracticeHistoryState = PracticeHistoryState(),
    replayActive: Boolean = true,
    savesPracticeOnDevice: Boolean = false,
    onPracticeCompleted: (PracticeRecord) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onExitParkedPractice: (() -> Unit)? = null,
    demoControls: @Composable () -> Unit = {}
) {
    val screenState = if (demo) {
        rememberSaveable { mutableStateOf(initialScreen) }
    } else {
        remember { mutableStateOf(initialScreen) }
    }
    var screen by screenState
    var scenario by rememberSaveable { mutableStateOf(DemoScenario.Smooth) }
    var replayedSamples by rememberSaveable {
        mutableIntStateOf(if (demo && initialScreen == TrainingScreen.Result) scenario.speeds.size else 0)
    }
    var vehicleResult by remember { mutableStateOf<DecelerationResult?>(null) }
    var showTurnSignalPractice by rememberSaveable { mutableStateOf(false) }
    val replayFinished = replayedSamples >= scenario.speeds.size
    val scrollState = rememberScrollState()

    fun startStopDemo() {
        if (screen != TrainingScreen.Home || showTurnSignalPractice) return
        replayedSamples = 0
        screen = TrainingScreen.Session
    }

    if (demo && !drivingRestricted && showTurnSignalPractice) {
        TurnSignalPractice(
            onExit = { showTurnSignalPractice = false },
            onCompleted = onPracticeCompleted
        )
        return
    }

    LaunchedEffect(screen, scenario, replayActive) {
        if (screen == TrainingScreen.Session && demo && replayActive) {
            if (replayedSamples == 0) replayedSamples = 1
            while (replayedSamples < scenario.speeds.size) {
                delay(scenario.replayDelayMillis)
                replayedSamples++
            }
        }
    }
    LaunchedEffect(screen, drivingRestricted) { scrollState.scrollTo(0) }

    BackHandler(enabled = !drivingRestricted && screen != TrainingScreen.Home) {
        if (!demo && screen == TrainingScreen.Session) cancelVehicleSession()
        screen = TrainingScreen.Home
    }

    val shownScreen = screen
    val result = if (demo) {
        remember(shownScreen, scenario, replayFinished) {
            if (shownScreen == TrainingScreen.Result && replayFinished) evaluateStopDemo(scenario) else null
        }
    } else vehicleResult

    val content = if (drivingRestricted) null else when (shownScreen) {
        TrainingScreen.Home -> ScreenContent(
            title = "운전 연수",
            description = vehicleStatus,
            cardTitle = "차량 속도 기록",
            cardText = "주차 중 연수를 시작하고, 다시 주차한 뒤 결과를 확인하세요.",
            action = "연수 시작"
        )
        TrainingScreen.Session -> ScreenContent(
            title = "연수 진행 중",
            description = if (demo) "모의 속도 재생 중" else "차량 속도 수집 중",
            cardTitle = "현재 속도",
            cardText = (if (demo) scenario.speeds.getOrNull(replayedSamples - 1) else speedSample?.metersPerSecond)
                ?.let { String.format(Locale.getDefault(), "%.1f km/h", it * 3.6f) }
                ?: "속도 데이터 대기 중",
            action = if (demo && !replayFinished) "데이터 재생 중…" else "연수 종료 및 결과 보기"
        )
        TrainingScreen.Result -> ScreenContent(
            title = "연수 결과",
            description = if (demo) "모의 정차 관찰" else "차량 속도 기반 관찰",
            cardTitle = when {
                result?.strongEventCount != null -> "감속 관찰"
                result?.stopObservations?.isNotEmpty() == true -> "정차 관찰"
                else -> "평가 보류"
            },
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
            val wide = maxWidth >= 840.dp
            val inset = if (maxWidth >= 1000.dp) 40.dp else 24.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(inset),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val label = when {
                    onExitParkedPractice != null -> "DRIVE COACH · 주차 모의 연수"
                    demo -> "DRIVE COACH · 모의 연수"
                    else -> "DRIVE COACH · 차량 연동"
                }
                Text(label, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    content == null -> {
                        Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(if (restrictionKnown) "주행 중 이용 제한" else "차량 상태 확인 중",
                                    style = MaterialTheme.typography.titleLarge)
                                Text("이용 가능한 차량 상태가 확인되면 화면이 다시 열려요.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    demo && shownScreen == TrainingScreen.Home -> {
                        PracticeDashboard(
                            wide = wide,
                            scenario = scenario,
                            history = practiceHistory,
                            savesOnDevice = savesPracticeOnDevice,
                            onChooseScenario = { scenario = it },
                            onStartStop = ::startStopDemo,
                            onOpenLaneChange = { showTurnSignalPractice = true },
                            onOpenRecommendation = { focus ->
                                if (focus.lesson == PracticeLesson.StopControl) {
                                    scenario = DemoScenario.Repeated
                                    startStopDemo()
                                } else {
                                    showTurnSignalPractice = true
                                }
                            },
                            onClearHistory = onClearHistory,
                            onExitParkedPractice = onExitParkedPractice
                        )
                    }
                    else -> {
                        TrainingScreenContent(
                            content, wide,
                            actionEnabled = when (shownScreen) {
                                TrainingScreen.Home -> speedSample != null
                                TrainingScreen.Session -> !demo || replayFinished
                                TrainingScreen.Result -> true
                            },
                            options = { if (shownScreen == TrainingScreen.Home) vehicleOptions() },
                            resultDetails = {
                                if (shownScreen == TrainingScreen.Result) {
                                    result?.stopObservations?.let { StopControlDetails(it) }
                                }
                            }
                        ) {
                            if (screen != shownScreen) return@TrainingScreenContent
                            when (shownScreen) {
                                TrainingScreen.Home -> {
                                    if (!startVehicleSession()) return@TrainingScreenContent
                                    vehicleResult = null
                                    screen = TrainingScreen.Session
                                }
                                TrainingScreen.Session -> {
                                    if (demo) {
                                        if (!replayFinished) return@TrainingScreenContent
                                        screen = TrainingScreen.Result
                                        PracticeRecord.fromStops(evaluateStopDemo(scenario))?.let(onPracticeCompleted)
                                    } else {
                                        val completed = finishVehicleSession() ?: return@TrainingScreenContent
                                        vehicleResult = completed
                                        screen = TrainingScreen.Result
                                    }
                                }
                                TrainingScreen.Result -> screen = TrainingScreen.Home
                            }
                        }
                    }
                }
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { demoControls() }
            }
        }
    }
}

private fun evaluateStopDemo(scenario: DemoScenario): DecelerationResult {
    val session = DecelerationSession()
    scenario.speeds.indices.forEach { session.add(scenario.sample(it)) }
    return session.finish(includeDemoScore = false)
}

@Composable
private fun TrainingScreenContent(
    content: ScreenContent,
    wide: Boolean,
    actionEnabled: Boolean = true,
    options: @Composable () -> Unit = {},
    resultDetails: @Composable () -> Unit = {},
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
            resultDetails()
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
private fun StopControlDetails(observations: List<StopControlObservation>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("정차 제어 관찰", style = MaterialTheme.typography.titleSmall)
            if (observations.isEmpty()) {
                Text("조건에 맞는 정차 기록이 부족해요.", style = MaterialTheme.typography.bodyMedium)
            }
            observations.forEach { observation ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("접근 속도 ${observation.speedBand.label}", style = MaterialTheme.typography.labelLarge)
                    val observed = observation.observedStops
                    val withoutCandidate = observation.stopsWithoutStrongCandidate
                    val fraction = "강한 감속 후보 미감지 ${withoutCandidate}/${observed}회"
                    if (observed >= MIN_COMPARABLE_STOPS) {
                        val percent = (100.0 * withoutCandidate / observed).roundToInt()
                        Text("$fraction ($percent%)", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(fraction, style = MaterialTheme.typography.bodyMedium)
                        val remaining = MIN_COMPARABLE_STOPS - observed
                        Text("비교에는 같은 속도대 기록이 ${remaining}회 더 필요해요.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit) {
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
internal fun SecondaryAction(label: String, onClick: () -> Unit) {
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
