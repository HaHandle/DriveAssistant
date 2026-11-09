package com.example.drivingcoach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class SignalScene(val meters: Int, val title: String, val description: String) {
    Approach(0, "변경까지 60m", "미러로 주변 차량을 확인하는 장면이에요."),
    BeforeWindow(25, "변경까지 35m", "옆 차로의 상황을 보면서 이동을 준비해요."),
    NearWindow(35, "변경까지 25m", "옆 차로 뒤에서 차가 다가와 간격이 부족해요."),
    Start(60, "차로 변경 시작", "접근하던 차가 지나가고 안전한 간격이 생겼어요."),
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

private val initialSignal = IndicatorObservation(TravelPoint(0L, 0.0), IndicatorState.Off)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TurnSignalPractice(onExit: () -> Unit) {
    var side by remember { mutableStateOf<LaneChangeSide?>(null) }
    var sceneIndex by remember { mutableIntStateOf(0) }
    var indicator by remember { mutableStateOf(IndicatorState.Off) }
    var gapChoice by remember { mutableStateOf<GapChoice?>(null) }
    var observations by remember { mutableStateOf(listOf(initialSignal)) }
    var report by remember { mutableStateOf<LaneChangeSignalReport?>(null) }
    val scrollState = rememberScrollState()
    LaunchedEffect(side, sceneIndex, report) { scrollState.scrollTo(0) }

    fun beginRound(direction: LaneChangeSide) {
        side = direction
        sceneIndex = 0
        indicator = IndicatorState.Off
        gapChoice = null
        observations = listOf(initialSignal)
        report = null
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
                        Text("깜빡이를 조작하거나 그대로 두고 다음 장면으로 넘어가세요.",
                            style = MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryAction("왼쪽 연수 시작") { beginRound(LaneChangeSide.Left) }
                            SecondaryAction("오른쪽 연수 시작") { beginRound(LaneChangeSide.Right) }
                        }
                    }
                    completed != null -> {
                        SignalReview(completed)
                        gapChoice?.let { choice ->
                            val detail = if (choice == GapChoice.Wait) {
                                "충족. 접근 차량이 지나갈 때까지 기다리기로 했어요."
                            } else {
                                "복기. 옆 차로에 충분한 간격이 생길 때까지 기다려요."
                            }
                            PracticeCard("안전 간격 판단", detail)
                        }
                        Text(nextPracticeTip(completed, gapChoice), style = MaterialTheme.typography.bodySmall)
                        PrimaryAction("다시 연습", enabled = true) { side = null }
                    }
                    else -> {
                        val scene = SignalScene.entries[sceneIndex]
                        Text("${direction.indicator.label} 차로 변경 · ${sceneIndex + 1}/${SignalScene.entries.size}",
                            style = MaterialTheme.typography.labelLarge)
                        PracticeCard(scene.title, scene.description) {
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
                            if (report != null || scene != SignalScene.entries[sceneIndex] ||
                                (asksAboutGap && gapChoice == null)
                            ) return@PrimaryAction
                            val recorded = observations + IndicatorObservation(scene.point, indicator)
                            observations = recorded
                            if (isLast) {
                                val window = LaneChangeWindow(direction, SignalScene.Start.point, SignalScene.Finish.point)
                                report = LaneChangeSignalEvaluator.evaluate(window, recorded)
                            } else {
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

private fun nextPracticeTip(report: LaneChangeSignalReport, gapChoice: GapChoice?): String = when {
    gapChoice == GapChoice.MoveNow -> "다음 연수에서는 접근하는 차량을 먼저 보내 보세요."
    report.onset != SignalOnset.InAdvance ->
        "변경 ${LaneChangeSignalEvaluator.MIN_LEAD_METERS}m 전까지 맞는 방향을 켜 보세요."
    report.hold != SignalHold.Maintained -> "이동이 끝날 때까지 맞는 방향을 유지해 보세요."
    report.clear != SignalClear.InTime -> "새 차로에 자리 잡으면 바로 신호를 꺼 보세요."
    else -> "반대 방향도 연습해 보세요. 이동 직전에 사각지대와 간격을 다시 확인하세요."
}

@Composable
private fun PracticeCard(
    title: String,
    description: String,
    content: @Composable () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            content()
        }
    }
}
