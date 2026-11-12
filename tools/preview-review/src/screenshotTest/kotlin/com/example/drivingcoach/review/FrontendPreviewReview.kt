package com.example.drivingcoach.review

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.ui.HabitAnalysisScreen
import com.example.drivingcoach.ui.ScoreTrendPanel
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.toHabitAnalysisPresentation
import com.example.drivingcoach.ui.preview.DriverOverviewPreviewParameterProvider
import com.example.drivingcoach.ui.preview.HabitAnalysisPreviewProvider
import com.example.drivingcoach.ui.preview.ScoreDashboardPreviewCase
import com.example.drivingcoach.ui.preview.ScoreDashboardPreviewProvider
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import com.example.drivingcoach.vehicle.VehicleUxState
import java.time.ZoneId

@Composable
private fun HabitContent(habit: DrivingHabitAnalysis) {
    DrivingCoachUITheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            HabitAnalysisScreen(habit.toHabitAnalysisPresentation(), onBack = {})
        }
    }
}

@PreviewTest @Preview(widthDp = 520, heightDp = 1000, locale = "ko")
@Composable fun HabitDay(@PreviewParameter(HabitAnalysisPreviewProvider::class) habit: DrivingHabitAnalysis) = HabitContent(habit)

@PreviewTest @Preview(widthDp = 520, heightDp = 1000, locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun HabitNight(@PreviewParameter(HabitAnalysisPreviewProvider::class) habit: DrivingHabitAnalysis) = HabitContent(habit)

@PreviewTest @Preview(widthDp = 320, heightDp = 1400, locale = "ko", fontScale = 2f)
@Composable fun HabitLargeText(@PreviewParameter(HabitAnalysisPreviewProvider::class) habit: DrivingHabitAnalysis) = HabitContent(habit)

@PreviewTest @Preview(widthDp = 1280, heightDp = 900, locale = "en")
@Composable fun HabitEnglish(@PreviewParameter(HabitAnalysisPreviewProvider::class) habit: DrivingHabitAnalysis) = HabitContent(habit)

@Composable
private fun ScoreContent(sample: ScoreDashboardPreviewCase) {
    var period by rememberSaveable(sample.name) { mutableStateOf(sample.period) }
    DrivingCoachUITheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(sample.name, style = MaterialTheme.typography.titleMedium)
                ScoreTrendPanel(sample.observations, period, { period = it }, zoneId = ZoneId.of("Asia/Seoul"))
            }
        }
    }
}

@PreviewTest @Preview(widthDp = 520, heightDp = 1000, locale = "ko")
@Composable fun ScoreDay(@PreviewParameter(ScoreDashboardPreviewProvider::class) sample: ScoreDashboardPreviewCase) = ScoreContent(sample)

@PreviewTest @Preview(widthDp = 520, heightDp = 1000, locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun ScoreNight(@PreviewParameter(ScoreDashboardPreviewProvider::class) sample: ScoreDashboardPreviewCase) = ScoreContent(sample)

@PreviewTest @Preview(widthDp = 320, heightDp = 1100, locale = "ko", fontScale = 2f)
@Composable fun ScoreLargeText(@PreviewParameter(ScoreDashboardPreviewProvider::class) sample: ScoreDashboardPreviewCase) = ScoreContent(sample)

@PreviewTest @Preview(widthDp = 960, heightDp = 800, locale = "en")
@Composable fun ScoreEnglish(@PreviewParameter(ScoreDashboardPreviewProvider::class) sample: ScoreDashboardPreviewCase) = ScoreContent(sample)

@PreviewTest @Preview(widthDp = 520, heightDp = 1100, locale = "ko")
@Composable fun AppDay(@PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState) =
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true, overviewState = state)

@PreviewTest @Preview(widthDp = 520, heightDp = 1100, locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun AppNight(@PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState) =
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true, overviewState = state)

@PreviewTest @Preview(widthDp = 520, heightDp = 1100, locale = "ko", fontScale = 2f)
@Composable fun AppLargeText(@PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState) =
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true, overviewState = state)

@PreviewTest @Preview(widthDp = 1280, heightDp = 900, locale = "en")
@Composable fun AppEnglish(@PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState) =
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true, overviewState = state)
