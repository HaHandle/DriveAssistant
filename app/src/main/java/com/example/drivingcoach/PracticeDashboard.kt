package com.example.drivingcoach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
internal fun PracticeDashboard(
    wide: Boolean,
    scenario: DemoScenario,
    history: PracticeHistoryState,
    savesOnDevice: Boolean,
    onChooseScenario: () -> Unit,
    onStartStop: () -> Unit,
    onOpenLaneChange: () -> Unit,
    onOpenRecommendation: (PracticeFocus) -> Unit,
    onClearHistory: () -> Unit,
    onExitParkedPractice: (() -> Unit)?
) {
    Text("운전 연수", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
    Text("오늘 연습할 습관을 골라 보세요.", style = MaterialTheme.typography.bodyMedium)

    val stopCard: @Composable (Modifier) -> Unit = { modifier ->
        PracticePanel("정차 제어", modifier) {
            Text("비슷한 접근 속도에서 멈추는 습관을 비교해요.",
                style = MaterialTheme.typography.bodyMedium)
            SecondaryAction("시나리오 · " + scenario.label, onChooseScenario)
            PrimaryAction("연수 시작", enabled = true, onClick = onStartStop)
        }
    }
    val laneCard: @Composable (Modifier) -> Unit = { modifier ->
        PracticePanel("차로 변경", modifier) {
            Text("미리 신호를 켜고 안전한 간격을 고르는 연습이에요.",
                style = MaterialTheme.typography.bodyMedium)
            PrimaryAction("방향지시등 연수", enabled = true, onClick = onOpenLaneChange)
        }
    }

    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top) {
            stopCard(Modifier.weight(1f))
            laneCard(Modifier.weight(1f))
        }
    } else {
        stopCard(Modifier)
        laneCard(Modifier)
    }

    history.records.firstOrNull()?.focus?.let { focus ->
        PracticePanel("다음 연습") {
            Text(focus.guidance, style = MaterialTheme.typography.bodyMedium)
            SecondaryAction("추천 연수 열기") { onOpenRecommendation(focus) }
        }
    }

    PracticeHistoryPanel(history, savesOnDevice, onClearHistory)
    if (onExitParkedPractice != null) {
        SecondaryAction("차량 연수로 돌아가기", onExitParkedPractice)
    }
}

@Composable
private fun PracticeHistoryPanel(
    history: PracticeHistoryState,
    savesOnDevice: Boolean,
    onClearHistory: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    PracticePanel("최근 연수") {
        Text(if (savesOnDevice) "최근 24회 요약을 이 기기에만 저장해요." else "이번 주차 연수의 요약이에요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        history.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (history.records.isEmpty() && history.message == null) {
            Text(if (history.busy) "기록을 불러오는 중이에요." else "연수를 마치면 여기에서 결과를 볼 수 있어요.",
                style = MaterialTheme.typography.bodyMedium)
        }
        val visibleRecords = if (showAll) history.records else history.records.take(3)
        visibleRecords.forEach { record ->
            val title = record.evidence.qualifier?.let { "${record.evidence.lesson.title} · $it" }
                ?: record.evidence.lesson.title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(record.resultText, style = MaterialTheme.typography.bodyMedium)
                Text(dateFormat.format(Date(record.completedAtMillis)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (history.records.size > 3) {
            SecondaryAction(if (showAll) "최근 3회만 보기" else "모든 최근 기록 보기") { showAll = !showAll }
        }
        if (savesOnDevice && !history.busy && (history.records.isNotEmpty() || history.message != null)) {
            SecondaryAction("연수 기록 지우기") { confirmClear = true }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("연수 기록을 지울까요?") },
            text = { Text("이 기기에 저장된 연수 요약을 모두 지워요.") },
            confirmButton = {
                SecondaryAction("지우기") {
                    confirmClear = false
                    onClearHistory()
                }
            },
            dismissButton = { SecondaryAction("취소") { confirmClear = false } }
        )
    }
}

@Composable
internal fun PracticePanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}
