package com.example.drivingcoach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class SignalScene(val meters: Int, val title: String, val description: String) {
    Approach(0, "변경까지 60m", "미러로 주변 차량을 확인하는 장면이에요."),
    BeforeWindow(25, "변경까지 35m", "옆 차로의 상황을 보면서 이동을 준비해요."),
    NearWindow(35, "변경까지 25m", ""),
    Start(60, "차로 변경 시작", ""),
    Moving(75, "차로 변경 중", "아직 옆 차로로 이동하는 중이에요."),
    Finish(90, "차로 변경 완료", "차가 새 차로에 자리 잡았어요."),
    AfterSeven(97, "변경 후 7m", "새 차로를 따라 계속 이동해요."),
    AfterThirteen(103, "변경 후 13m", "이 장면을 마치면 연수 결과를 확인할 수 있어요.");

    val point: TravelPoint
        get() = TravelPoint(100L + meters * 100L, meters.toDouble())
}

private enum class GapChoice(val label: String) {
    Wait("기다리기"), MoveNow("지금 이동하기")
}

private enum class GapScenario(
    val label: String,
    val traffic: String,
    val safeStart: String,
    val clearanceConfirmed: Boolean
) {
    Approaching(
        "뒤차 접근",
        "옆 차로 뒤에서 차가 다가와 간격이 부족해요.",
        "접근하던 차가 지나가고 안전한 간격이 생겼어요.",
        false
    ),
    Obstructed(
        "시야 가림",
        "옆 차로가 가려져 차량이 있는지 확인하기 어려워요.",
        "시야가 열리고 옆 차로의 안전한 간격을 확인했어요.",
        false
    ),
    Clear(
        "간격 확인",
        "미러와 사각지대를 확인했어요. 옆 차로 앞뒤에 충분한 간격이 있어요.",
        "확인한 간격으로 이동을 시작해요.",
        true
    );

    fun assess(choice: GapChoice): GapOutcome = when (choice) {
        GapChoice.Wait -> if (clearanceConfirmed) GapOutcome.ChoseExtraWait else GapOutcome.WaitedForClearance
        GapChoice.MoveNow -> if (clearanceConfirmed) GapOutcome.MovedAfterCheck else GapOutcome.MovedWithoutClearance
    }
}

private val initialSignal = IndicatorObservation(TravelPoint(0L, 0.0), IndicatorState.Off)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TurnSignalPractice(
    onExit: () -> Unit,
    onCompleted: (PracticeRecord) -> Unit
) {
    var gapScenario by rememberSaveable { mutableStateOf(GapScenario.Approaching) }
    var scenarioMenuOpen by remember { mutableStateOf(false) }
    var side by rememberSaveable { mutableStateOf<LaneChangeSide?>(null) }
    var sceneIndex by rememberSaveable { mutableIntStateOf(0) }
    var indicator by rememberSaveable { mutableStateOf(IndicatorState.Off) }
    var gapChoice by rememberSaveable { mutableStateOf<GapChoice?>(null) }
    var recordedStates by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var roundFinished by rememberSaveable { mutableStateOf(false) }
    val report = remember(side, roundFinished, recordedStates) {
        val direction = side
        if (roundFinished && direction != null) evaluateSignalRound(direction, recordedStates) else null
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(side, sceneIndex, roundFinished) { scrollState.scrollTo(0) }

    fun beginRound(direction: LaneChangeSide) {
        side = direction
        sceneIndex = 0
        indicator = IndicatorState.Off
        gapChoice = null
        recordedStates = arrayListOf()
        roundFinished = false
    }

    BackHandler(onBack = onExit)
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val inset = if (maxWidth >= 1000.dp) 40.dp else 24.dp
            Column(
                Modifier.widthIn(max = 740.dp).fillMaxWidth()
                    .verticalScroll(scrollState).padding(inset),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("정차 상태에서 하는 모의 연수", style = MaterialTheme.typography.labelLarge)
                Text("방향지시등 연수", style = MaterialTheme.typography.titleLarge)

                val direction = side
                val completed = report
                when {
                    direction == null -> {
                        val lead = LaneChangeSignalEvaluator.MIN_LEAD_METERS
                        PracticeCard("옆 차로로 이동하기", "미러를 보고 변경 ${lead}m 전까지 신호를 켜요.") {
                            Text("이동 직전에 사각지대와 간격을 다시 확인해요.",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("여유가 없으면 기다리고, 이동이 끝나면 신호를 꺼요.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("상황을 고른 뒤 깜빡이를 조작하며 장면을 넘겨 보세요.",
                            style = MaterialTheme.typography.bodyMedium)
                        Box {
                            SecondaryAction("상황 · ${gapScenario.label}") { scenarioMenuOpen = true }
                            DropdownMenu(expanded = scenarioMenuOpen, onDismissRequest = { scenarioMenuOpen = false }) {
                                GapScenario.entries.forEach { scenario ->
                                    DropdownMenuItem(
                                        text = { Text(scenario.label, style = MaterialTheme.typography.labelLarge) },
                                        modifier = Modifier.heightIn(min = 64.dp),
                                        onClick = {
                                            gapScenario = scenario
                                            scenarioMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryAction("왼쪽 연수 시작") { beginRound(LaneChangeSide.Left) }
                            SecondaryAction("오른쪽 연수 시작") { beginRound(LaneChangeSide.Right) }
                        }
                    }
                    completed != null -> {
                        SignalReview(completed)
                        gapChoice?.let { choice ->
                            val outcome = gapScenario.assess(choice)
                            GapReview(outcome)
                            Text(PracticeRecord.focusForLaneChange(completed, outcome).guidance,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        PrimaryAction("다시 연습", enabled = true) { side = null }
                    }
                    else -> {
                        val scene = SignalScene.entries[sceneIndex]
                        Text("${direction.indicator.label} 차로 변경 · ${sceneIndex + 1}/${SignalScene.entries.size}",
                            style = MaterialTheme.typography.labelLarge)
                        val description = when (scene) {
                            SignalScene.NearWindow -> gapScenario.traffic
                            SignalScene.Start -> gapScenario.safeStart
                            else -> scene.description
                        }
                        PracticeCard(scene.title, description) {
                            Text("켜진 신호 · ${indicator.label}", style = MaterialTheme.typography.labelLarge)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryAction("왼쪽 켜기") { indicator = IndicatorState.Left }
                            SecondaryAction("오른쪽 켜기") { indicator = IndicatorState.Right }
                            SecondaryAction("끄기") { indicator = IndicatorState.Off }
                        }
                        val asksAboutGap = scene == SignalScene.NearWindow
                        if (asksAboutGap) {
                            Text("지금이라면 어떻게 할까요?", style = MaterialTheme.typography.bodyMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GapChoice.entries.forEach { choice ->
                                    SecondaryAction(choice.label) { gapChoice = choice }
                                }
                            }
                            gapChoice?.let {
                                Text("선택 · ${it.label}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        val isLast = scene == SignalScene.entries.last()
                        val canAdvance = !asksAboutGap || gapChoice != null
                        PrimaryAction(if (isLast) "결과 보기" else "다음 장면", enabled = canAdvance) {
                            if (roundFinished || scene != SignalScene.entries[sceneIndex] ||
                                (asksAboutGap && gapChoice == null)
                            ) return@PrimaryAction
                            val recorded = ArrayList(recordedStates).apply { add(indicator.name) }
                            if (isLast) {
                                val choice = gapChoice ?: return@PrimaryAction
                                val assessment = evaluateSignalRound(direction, recorded)
                                recordedStates = recorded
                                roundFinished = true
                                onCompleted(PracticeRecord.fromLaneChange(assessment, gapScenario.assess(choice)))
                            } else {
                                recordedStates = recorded
                                sceneIndex++
                            }
                        }
                    }
                }

                SecondaryAction("연수 홈으로", onExit)
            }
        }
    }
}

private fun evaluateSignalRound(direction: LaneChangeSide, states: List<String>): LaneChangeSignalReport {
    val scenes = SignalScene.entries
    if (states.size != scenes.size) return LaneChangeSignalReport()
    val observations = listOf(initialSignal) + states.mapIndexed { index, state ->
        val indicator = IndicatorState.entries.firstOrNull { it.name == state } ?: IndicatorState.Unavailable
        IndicatorObservation(scenes[index].point, indicator)
    }
    val window = LaneChangeWindow(direction, SignalScene.Start.point, SignalScene.Finish.point)
    return LaneChangeSignalEvaluator.evaluate(window, observations)
}

@Composable
private fun SignalReview(report: LaneChangeSignalReport) {
    val leadDistance = report.leadMeters?.roundToInt()
    val onsetDetail = when (report.onset) {
        SignalOnset.InAdvance -> leadDistance?.let { "충족. 변경 ${it}m 전에 켰어요." }
            ?: "충족. 변경 전에 미리 켰어요."
        SignalOnset.Late -> when {
            leadDistance == null -> "복기. 변경을 시작한 뒤에 켰어요."
            leadDistance == 0 -> "복기. 변경이 시작될 때 켰어요."
            else -> "복기. 변경 ${leadDistance}m 전에 켰어요."
        }
        SignalOnset.WrongDirection -> "복기. 변경 시작 때 반대 방향이 켜져 있었어요."
        SignalOnset.Missing -> "복기. 바꿀 방향의 신호를 켜지 않았어요."
        SignalOnset.Unobserved -> "관찰 부족. 신호를 켠 시점을 확인할 수 없어요."
    }
    val holdDetail = when (report.hold) {
        SignalHold.Maintained -> "충족. 완료까지 같은 신호를 유지했어요."
        SignalHold.Interrupted -> "복기. 처음부터 끝까지 같은 신호를 켜요."
        SignalHold.Unobserved -> "관찰 부족. 변경 중 신호를 확인할 수 없어요."
    }
    val clearDetail = when (report.clear) {
        SignalClear.InTime -> report.clearMeters?.roundToInt()?.takeIf { it > 0 }
            ?.let { "충족. 변경 후 ${it}m에서 껐어요." }
            ?: "충족. 이동이 끝날 때 신호를 껐어요."
        SignalClear.LeftOn -> "복기. 완료 뒤 ${LaneChangeSignalEvaluator.CLEAR_BEFORE_METERS}m 이상 신호가 켜져 있었어요."
        SignalClear.OtherDirection -> "복기. 이동이 끝난 뒤 반대 방향이 켜졌어요."
        SignalClear.Reactivated -> "복기. 이동이 끝난 뒤 신호가 다시 켜졌어요."
        SignalClear.Unobserved -> "관찰 부족. 완료 후 신호를 끈 시점을 확인할 수 없어요."
    }
    PracticeCard("신호 습관 결과", "미리 켜기 · $onsetDetail") {
        Text("변경 중 유지 · $holdDetail", style = MaterialTheme.typography.bodyMedium)
        Text("완료 후 끄기 · $clearDetail", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GapReview(outcome: GapOutcome) {
    val detail = when (outcome) {
        GapOutcome.WaitedForClearance -> "충족. 안전한 간격이 확인될 때까지 기다렸어요."
        GapOutcome.MovedAfterCheck -> "충족. 간격을 확인하고 이동했어요."
        GapOutcome.ChoseExtraWait -> "충족. 간격이 있어도 더 기다려도 돼요."
        GapOutcome.MovedWithoutClearance -> "복기. 옆 차로가 확인될 때까지 기다려요."
    }
    PracticeCard("안전 간격 판단", detail)
}

@Composable
private fun PracticeCard(
    title: String,
    description: String,
    content: @Composable () -> Unit = {}
) {
    PracticePanel(title) {
        Text(description, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}
