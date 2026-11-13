package com.example.drivingcoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

data class TrainingResultDisplay(
    val feedback: String,
    val score: Int?,
    val strongEvents: Int?,
    val peakDeceleration: String?,
    val stopSummary: String,
)

data class TrainingUiState(
    val automotive: Boolean = false,
    val permissionGranted: Boolean = false,
    val speedKmh: Double? = null,
    val active: Boolean = false,
    val result: TrainingResultDisplay? = null,
)

/** A separate practice destination so the overview's score and habit records remain untouched. */
@Composable
fun SpeedTrainingScreen(
    state: TrainingUiState,
    onRequestPermission: () -> Unit,
    onStart: (Int) -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp)
            .testTag("speed-training"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("정차 제어 연수", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (state.automotive) "차량 속도 기반 관찰 · 파일럿 기준"
            else "속도 시나리오 데모 · 실제 주행 기록이 아닙니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        when {
            state.result != null -> {
                val result = state.result
                Text(if (result.strongEvents == null) "평가 보류" else "감속 관찰",
                    style = MaterialTheme.typography.titleLarge)
                result.score?.let { Text("데모용 감속 점수 $it 점") }
                result.strongEvents?.let { Text("강한 감속 후보 $it 회") }
                result.peakDeceleration?.let { Text("구간 최대 감속 $it") }
                if (result.stopSummary.isNotBlank()) Text(result.stopSummary)
                Text(result.feedback)
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("다시 연습") }
            }
            state.active -> {
                Text("차량 속도 수집 중", style = MaterialTheme.typography.titleLarge)
                Text(state.speedKmh?.let { String.format(Locale.getDefault(), "%.1f km/h", it) }
                    ?: "속도 데이터 수신 대기")
                Text("주차 상태로 돌아온 뒤 결과를 확인하세요.")
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("연수 종료 및 결과 보기")
                }
            }
            state.automotive -> {
                Text(state.speedKmh?.let { String.format(Locale.getDefault(), "최근 속도 %.1f km/h", it) }
                    ?: "속도 데이터 수신 대기")
                if (!state.permissionGranted) {
                    Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("속도 데이터 권한 허용")
                    }
                }
                Button(onClick = { onStart(0) },
                    enabled = state.permissionGranted && state.speedKmh != null,
                    modifier = Modifier.fillMaxWidth()) { Text("연수 시작") }
                Text("속도 신호가 끊기거나 앱이 중단되면 평가를 보류합니다.")
            }
            else -> {
                listOf("부드러운 감속", "급한 감속", "반복 정차").forEachIndexed { index, label ->
                    Button(onClick = { onStart(index) }, modifier = Modifier.fillMaxWidth()) {
                        Text("$label 데모")
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("코칭 홈으로") }
    }
}
