package com.example.drivingcoach.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import com.example.drivingcoach.ui.ScoreTrendPanel
import com.example.drivingcoach.ui.model.DrivingScore
import com.example.drivingcoach.ui.model.ScoreObservation
import com.example.drivingcoach.ui.model.ScorePeriod
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import java.time.ZoneId

data class ScoreDashboardPreviewCase(
    val name: String,
    val observations: List<ScoreObservation>,
    val period: ScorePeriod,
)

class ScoreDashboardPreviewProvider : PreviewParameterProvider<ScoreDashboardPreviewCase> {
    override val values: Sequence<ScoreDashboardPreviewCase>
        get() = sequence {
            val normal = (MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready)
                .overview.score.trend
            val partial = (MockDriverOverviewData.state(OverviewScenario.PARTIAL) as DriverOverviewState.Ready)
                .overview.score.trend
            for (period in ScorePeriod.entries) yield(ScoreDashboardPreviewCase(period.name, normal, period))
            yield(ScoreDashboardPreviewCase("Missing endpoints and gaps", partial, ScorePeriod.WEEK))
            yield(ScoreDashboardPreviewCase("No history", emptyList(), ScorePeriod.WEEK))
            yield(ScoreDashboardPreviewCase("Only missing measurements",
                normal.takeLast(3).map { it.copy(score = null) }, ScorePeriod.WEEK))
            yield(ScoreDashboardPreviewCase("One measured zero",
                listOf(normal.last().copy(score = DrivingScore(0))), ScorePeriod.ALL))
            yield(ScoreDashboardPreviewCase("Zero and one hundred",
                normal.takeLast(2).mapIndexed { index, point -> point.copy(score = DrivingScore(index * 100)) },
                ScorePeriod.WEEK))
        }
}

@Preview(name = "Score dashboard · day", group = "Score dashboard", widthDp = 520, heightDp = 1000, locale = "ko")
@Preview(name = "Score dashboard · night", group = "Score dashboard", widthDp = 520, heightDp = 1000,
    locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Score dashboard · large text", group = "Score dashboard", widthDp = 320, heightDp = 1100,
    locale = "ko", fontScale = 2f)
@Preview(name = "Score dashboard · English", group = "Score dashboard", widthDp = 960, heightDp = 800, locale = "en")
@Composable
private fun ScoreDashboardPreview(
    @PreviewParameter(ScoreDashboardPreviewProvider::class) sample: ScoreDashboardPreviewCase,
) {
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
