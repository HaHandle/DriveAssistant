package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme

class MainActivity : ComponentActivity() {
    private val automotive by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
    private var restrictionsMonitor: CarRestrictionsMonitor? = null
    private var vehicleRestricted by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrivingCoachUITheme {
                if (automotive) {
                    TrainingFlow(
                        drivingRestricted = vehicleRestricted != false,
                        restrictionKnown = vehicleRestricted != null
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
            val monitor = CarRestrictionsMonitor(this) { vehicleRestricted = it }
            restrictionsMonitor = monitor
            monitor.start()
        }
    }

    override fun onStop() {
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

private val mockResult = ScreenContent(
    title = "연수 결과",
    description = "시뮬레이션 세션을 완료했어요.",
    cardTitle = "부드러운 감속 · 예시 점수 82점",
    cardText = "다음 연습에서는 정지 지점에 도달하기 전에 조금 더 일찍 감속해 보세요.",
    action = "홈으로"
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
    demoControls: @Composable () -> Unit = {}
) {
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    BackHandler(enabled = !drivingRestricted && screen != TrainingScreen.Home) {
        screen = TrainingScreen.Home
    }

    val content = if (drivingRestricted) null else when (screen) {
        TrainingScreen.Home -> ScreenContent(
            title = "운전 연수",
            description = "첫 연습으로 부드러운 감속을 체험해 보세요.",
            cardTitle = "기본 연수",
            cardText = "연습을 시작하고 종료하면 샘플 코칭 피드백을 볼 수 있어요.",
            action = "연수 시작"
        )
        TrainingScreen.Session -> ScreenContent(
            title = "연수 진행 중",
            description = "부드러운 감속 시나리오를 연습하고 있어요.",
            cardTitle = "연습 목표",
            cardText = "정지 지점에 다가갈 때 속도를 서서히 줄이는 상황을 연습해요.",
            action = "연수 종료 및 결과 보기"
        )
        TrainingScreen.Result -> mockResult
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
                    text = "DRIVE COACH · 데모",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (content == null) {
                    val title = if (restrictionKnown) "주행 중 이용 제한" else "차량 상태 확인 중"
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text("차량에서 이용 가능 상태가 확인되면 이어서 이용할 수 있어요.")
                } else {
                    TrainingScreenContent(content) {
                        screen = when (screen) {
                            TrainingScreen.Home -> TrainingScreen.Session
                            TrainingScreen.Session -> TrainingScreen.Result
                            TrainingScreen.Result -> TrainingScreen.Home
                        }
                    }
                }
                demoControls()
            }
        }
    }
}

@Composable
private fun TrainingScreenContent(content: ScreenContent, onAction: () -> Unit) {
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
