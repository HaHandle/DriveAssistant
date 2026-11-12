package com.example.drivingcoach.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import com.example.drivingcoach.ui.HabitAnalysisScreen
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.HabitKind
import com.example.drivingcoach.ui.model.HabitStatus
import com.example.drivingcoach.ui.model.ObservationCount
import com.example.drivingcoach.ui.model.toHabitAnalysisPresentation
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme

class HabitAnalysisPreviewProvider : PreviewParameterProvider<DrivingHabitAnalysis> {
    override val values: Sequence<DrivingHabitAnalysis>
        get() = sequence {
            val normal = MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready
            yieldAll(normal.overview.habits)
            val partial = MockDriverOverviewData.state(OverviewScenario.PARTIAL) as DriverOverviewState.Ready
            yieldAll(partial.overview.habits.filter { it.status == HabitStatus.UNOBSERVED })
            yield(DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(0, 3)))
            yield(DrivingHabitAnalysis(HabitKind.SIGNAL_TIMING, HabitStatus.UNOBSERVED, ObservationCount(3, 3)))
            yield(DrivingHabitAnalysis(HabitKind.SIGNAL_CANCEL, HabitStatus.MET, ObservationCount(0, 3)))
        }
}

@Preview(name = "Habits · day", group = "Habit analysis", widthDp = 520, heightDp = 1000, locale = "ko")
@Preview(name = "Habits · night", group = "Habit analysis", widthDp = 520, heightDp = 1000,
    locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Habits · large text", group = "Habit analysis", widthDp = 320, heightDp = 1400,
    locale = "ko", fontScale = 2f)
@Preview(name = "Habits · wide English", group = "Habit analysis", widthDp = 1280, heightDp = 900, locale = "en")
@Composable
private fun HabitAnalysisPreview(
    @PreviewParameter(HabitAnalysisPreviewProvider::class) habit: DrivingHabitAnalysis,
) {
    DrivingCoachUITheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            HabitAnalysisScreen(habit.toHabitAnalysisPresentation(), onBack = {})
        }
    }
}
