package com.example.drivingcoach.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.R
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import com.example.drivingcoach.ui.CoachPanel
import com.example.drivingcoach.ui.Eyebrow
import com.example.drivingcoach.ui.LiquidScore
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.ui.OverviewHabitsPanel
import com.example.drivingcoach.ui.model.DriverOverview
import com.example.drivingcoach.ui.model.DriverProgress
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import com.example.drivingcoach.vehicle.VehicleUxState

/** Direct state injection keeps previews independent of Activity, ViewModel and vehicle services. */
class DriverOverviewPreviewParameterProvider : PreviewParameterProvider<DriverOverviewState> {
    override val values: Sequence<DriverOverviewState>
        get() = OverviewScenario.entries.asSequence().map(MockDriverOverviewData::state)
}

@Preview(name = "App states · day", widthDp = 520, heightDp = 1100, locale = "ko")
@Preview(name = "App states · night", widthDp = 520, heightDp = 1100, locale = "ko",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "App states · large text", widthDp = 520, heightDp = 1100, locale = "ko", fontScale = 2f)
@Preview(name = "App states · wide English", widthDp = 1280, heightDp = 900, locale = "en")
@Composable
private fun DriverOverviewAppPreview(
    @PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState,
) {
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true, overviewState = state)
}

@Preview(name = "Overview data · day", widthDp = 520, heightDp = 1100, locale = "ko")
@Preview(name = "Overview data · night", widthDp = 520, heightDp = 1100, locale = "ko",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Overview data · wide English", widthDp = 960, heightDp = 1100, locale = "en")
@Composable
private fun DriverOverviewDataPreview(
    @PreviewParameter(DriverOverviewPreviewParameterProvider::class) state: DriverOverviewState,
) {
    DrivingCoachUITheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(stringResource(R.string.overview_preview_title), style = MaterialTheme.typography.headlineMedium)
                Eyebrow(stringResource(R.string.sample_data))
                when (state) {
                    DriverOverviewState.Loading -> PreviewNotice(R.string.overview_loading)
                    DriverOverviewState.Empty -> PreviewNotice(R.string.overview_empty)
                    is DriverOverviewState.Error -> PreviewNotice(R.string.overview_error)
                    is DriverOverviewState.Ready -> OverviewReadyPreview(state.overview)
                }
            }
        }
    }
}

@Composable
private fun PreviewNotice(message: Int) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OverviewReadyPreview(overview: DriverOverview) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.score_heading))
        val current = overview.score.current
        if (current == null) {
            Text(stringResource(R.string.overview_unmeasured), style = MaterialTheme.typography.titleLarge)
        } else {
            LiquidScore(current.value, stringResource(R.string.score_accessibility, current.value),
                stringResource(R.string.score_caption), Modifier.align(Alignment.CenterHorizontally))
        }
        val change = overview.score.change
        Text(if (change == null) stringResource(R.string.overview_no_comparison)
            else stringResource(R.string.overview_score_change, change),
            style = MaterialTheme.typography.bodyMedium)
    }
    OverviewHabitsPanel(overview.habits)
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.overview_progress))
        when (val progress = overview.progress) {
            DriverProgress.Unassigned -> Text(stringResource(R.string.overview_progress_unassigned),
                style = MaterialTheme.typography.bodyLarge)
            is DriverProgress.Preview -> {
                Text(stringResource(R.string.overview_progress_example), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.overview_progress_level, progress.level, progress.tierLabel),
                    style = MaterialTheme.typography.titleLarge)
                val required = progress.requiredWithinTier
                Text(if (required == null) stringResource(R.string.overview_progress_highest)
                    else stringResource(R.string.overview_progress_experience, progress.earnedWithinTier, required),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
