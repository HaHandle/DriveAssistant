package com.example.drivingcoach.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.R
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.ui.model.DriverOverview
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.HabitKind
import com.example.drivingcoach.ui.model.HabitStatus
import com.example.drivingcoach.ui.model.ScorePeriod

private val focusLabels = listOf(R.string.focus_balanced, R.string.focus_smooth, R.string.focus_efficient)
private val focusTitles = listOf(R.string.balanced_title, R.string.smooth_title, R.string.efficient_title)
private val focusBodies = listOf(R.string.balanced_body, R.string.smooth_body, R.string.efficient_body)
private val focusSymbols = listOf(CoachSymbol.SHIELD, CoachSymbol.WAVE, CoachSymbol.LEAF)
private val tripTitles = listOf(R.string.trip_city, R.string.trip_commute, R.string.trip_weekend)
private val tripMeta = listOf(R.string.trip_city_meta, R.string.trip_commute_meta, R.string.trip_weekend_meta)
private val tripScores = listOf(92, 88, 95)

@Composable
private fun PageHeading(title: Int, subtitle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(subtitle), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SampleLabel() {
    Text(stringResource(R.string.sample_data), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("sample-data-label"))
}

@Composable
fun CoachHomeScreen(
    sampleData: Boolean,
    overviewState: DriverOverviewState,
    scorePeriod: ScorePeriod,
    onScorePeriod: (ScorePeriod) -> Unit,
    onRetry: () -> Unit,
    focus: Int,
    previewActive: Boolean,
    onFocus: (Int) -> Unit,
    onPreview: () -> Unit,
    onHistory: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 940.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Eyebrow(stringResource(R.string.home_eyebrow))
            PageHeading(R.string.home_title, R.string.home_subtitle)
            if (sampleData) SampleLabel()
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        ScoreOverviewPanel(sampleData, overviewState, onRetry, focus)
                        if (sampleData && overviewState is DriverOverviewState.Ready) {
                            ScoreTrendPanel(overviewState.overview.score.trend, scorePeriod, onScorePeriod)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        CoachingPanel(sampleData, focus, previewActive, onFocus, onPreview)
                        if (sampleData && overviewState is DriverOverviewState.Ready) {
                            OverviewHabitsPanel(overviewState.overview.habits)
                        }
                        InsightPanel()
                        if (sampleData && overviewState is DriverOverviewState.Ready) LatestTrip(onHistory)
                    }
                }
            } else {
                ScoreOverviewPanel(sampleData, overviewState, onRetry, focus)
                if (sampleData && overviewState is DriverOverviewState.Ready) {
                    ScoreTrendPanel(overviewState.overview.score.trend, scorePeriod, onScorePeriod)
                }
                CoachingPanel(sampleData, focus, previewActive, onFocus, onPreview)
                if (sampleData && overviewState is DriverOverviewState.Ready) {
                    OverviewHabitsPanel(overviewState.overview.habits)
                }
                InsightPanel()
                if (sampleData && overviewState is DriverOverviewState.Ready) LatestTrip(onHistory)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ScoreOverviewPanel(
    sampleData: Boolean,
    overviewState: DriverOverviewState,
    onRetry: () -> Unit,
    focus: Int,
) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.score_heading))
        if (sampleData) {
            when (overviewState) {
                DriverOverviewState.Loading -> Text(stringResource(R.string.overview_loading),
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("overview-loading"))
                DriverOverviewState.Empty -> Text(stringResource(R.string.overview_empty),
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("overview-empty"))
                is DriverOverviewState.Error -> {
                    Text(stringResource(R.string.overview_error), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("overview-error"))
                    LiquidAction(stringResource(R.string.overview_retry), onRetry,
                        Modifier.fillMaxWidth().testTag("overview-retry"), secondary = true)
                }
                is DriverOverviewState.Ready -> OverviewScore(overviewState.overview, focus)
            }
        } else {
            CoachIcon(CoachSymbol.COACH, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.connection_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.connection_body), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CoachingPanel(
    sampleData: Boolean,
    focus: Int,
    previewActive: Boolean,
    onFocus: (Int) -> Unit,
    onPreview: () -> Unit,
) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.focus_heading))
        LiquidSelection(
            focusLabels.map { stringResource(it) }, focus, onFocus,
            Modifier.fillMaxWidth().testTag("mode-selector"),
        )
        val motion = LocalCoachMotion.current
        AnimatedContent(
            targetState = focus,
            transitionSpec = {
                if (motion) fadeIn(tween(180)) togetherWith fadeOut(tween(90))
                else EnterTransition.None togetherWith ExitTransition.None
            },
            label = "coachingFocus",
        ) { index ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoachIcon(focusSymbols[index], tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(focusTitles[index]), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(focusBodies[index]), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (sampleData && previewActive) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("preview-active")) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.preview_active), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.preview_active_body), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
        LiquidAction(
            label = stringResource(when {
                !sampleData -> R.string.connection_waiting
                previewActive -> R.string.preview_stop
                else -> R.string.preview_start
            }),
            onClick = onPreview,
            enabled = sampleData,
            symbol = if (previewActive) CoachSymbol.STOP else CoachSymbol.PLAY,
            modifier = Modifier.fillMaxWidth().testTag("coach-preview"),
        )
    }
}

@Composable
private fun OverviewScore(overview: DriverOverview, focus: Int) {
    Column(Modifier.fillMaxWidth().testTag("overview-ready"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val current = overview.score.current
        if (current == null) {
            Text(stringResource(R.string.overview_unmeasured), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("overview-unmeasured"))
        } else {
            LiquidScore(current.value, stringResource(R.string.score_accessibility, current.value),
                stringResource(R.string.score_caption), contour = focus)
        }
        val change = overview.score.change
        Text(if (change == null) stringResource(R.string.overview_no_comparison)
            else stringResource(R.string.overview_score_change, change),
            style = MaterialTheme.typography.bodyMedium)
    }
}

/** Shared by the app and direct-state previews; observations never become aggregate scores. */
@Composable
fun OverviewHabitsPanel(habits: List<DrivingHabitAnalysis>) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.overview_habits))
        if (habits.isEmpty()) {
            Text(stringResource(R.string.habit_unobserved), style = MaterialTheme.typography.bodyMedium)
        }
        habits.forEach { habit ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(when (habit.kind) {
                    HabitKind.STOP_CONTROL -> R.string.habit_stop_control
                    HabitKind.SIGNAL_TIMING -> R.string.habit_signal_timing
                    HabitKind.SIGNAL_HOLD -> R.string.habit_signal_hold
                    HabitKind.SIGNAL_CANCEL -> R.string.habit_signal_cancel
                }), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(when (habit.status) {
                    HabitStatus.MET -> R.string.habit_met
                    HabitStatus.REVIEW -> R.string.habit_review
                    HabitStatus.UNOBSERVED -> R.string.habit_unobserved
                }), style = MaterialTheme.typography.bodyMedium)
                val counts = habit.observations
                if (counts?.hasObservations == true) {
                    Text(stringResource(R.string.habit_observations, counts.met, counts.observed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel() {
    CoachPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.metrics_heading), style = MaterialTheme.typography.titleMedium)
        val labels = listOf(R.string.metric_smooth, R.string.metric_space, R.string.metric_efficient)
        val icons = listOf(CoachSymbol.WAVE, CoachSymbol.SHIELD, CoachSymbol.LEAF)
        labels.forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CoachIcon(icons[index], tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(if (index == 1) R.string.metric_stable else R.string.metric_good),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun InsightPanel() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CoachIcon(CoachSymbol.LEAF, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(stringResource(R.string.insight_eyebrow), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(stringResource(R.string.insight_title), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(stringResource(R.string.insight_body), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 3,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LatestTrip(onHistory: () -> Unit) {
    CoachPanel(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.latest_heading))
        Text(stringResource(R.string.trip_city), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.trip_city_meta), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LiquidAction(stringResource(R.string.view_history), onHistory,
            Modifier.fillMaxWidth(), secondary = true)
    }
}

@Composable
fun HistoryScreen(sampleData: Boolean, selectedTrip: Int, onSelectTrip: (Int) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (sampleData && selectedTrip in tripTitles.indices) {
            LiquidAction(stringResource(R.string.back_to_history), onBack,
                Modifier.fillMaxWidth(), CoachSymbol.BACK, secondary = true)
            SampleLabel()
            CoachPanel(Modifier.fillMaxWidth().testTag("trip-detail")) {
                Eyebrow(stringResource(R.string.trip_detail))
                Text(stringResource(tripTitles[selectedTrip]), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(tripMeta[selectedTrip]), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val score = tripScores[selectedTrip]
                LiquidScore(score, stringResource(R.string.score_accessibility, score),
                    stringResource(R.string.score_caption), Modifier.align(Alignment.CenterHorizontally))
            }
            MetricsPanel()
            CoachPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trip_note), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.trip_note_body), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            PageHeading(R.string.history_title, R.string.history_subtitle)
            if (sampleData) {
                SampleLabel()
                Column(Modifier.testTag("history-list"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    tripTitles.indices.forEach { index ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                Modifier.clickable(role = Role.Button) { onSelectTrip(index) }
                                    .heightIn(min = 116.dp).padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(tripTitles[index]),
                                        style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(tripMeta[index]), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(stringResource(R.string.points, tripScores[index]),
                                    style = MaterialTheme.typography.titleLarge)
                                CoachIcon(CoachSymbol.ARROW)
                            }
                        }
                    }
                }
            } else {
                CoachPanel(Modifier.fillMaxWidth()) {
                    CoachIcon(CoachSymbol.HISTORY, Modifier.size(48.dp))
                    Text(stringResource(R.string.empty_history_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.empty_history_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    theme: Int,
    motion: Boolean,
    haptics: Boolean,
    preview: Boolean,
    onTheme: (Int) -> Unit,
    onMotion: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onPreviewDriving: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PageHeading(R.string.settings_title, R.string.settings_subtitle)
        CoachPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.appearance_heading), style = MaterialTheme.typography.titleLarge)
            LiquidSelection(
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).map { stringResource(it) },
                theme, onTheme, Modifier.fillMaxWidth().testTag("theme-selector"),
            )
            Text(stringResource(R.string.theme_description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CoachPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.interaction_heading), style = MaterialTheme.typography.titleLarge)
            SettingToggle(R.string.motion_title, R.string.motion_description, motion, onMotion, "motion-switch")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingToggle(R.string.haptics_title, R.string.haptics_description, haptics, onHaptics, "haptics-switch")
            Text(stringResource(R.string.motion_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (preview) {
            CoachPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.preview_heading), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.preview_note), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LiquidAction(stringResource(R.string.preview_driving_title), onPreviewDriving,
                    Modifier.fillMaxWidth().testTag("preview-driving"), CoachSymbol.SHIELD, secondary = true)
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: Int, description: Int, checked: Boolean, onChange: (Boolean) -> Unit, tag: String,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).testTag(tag)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
fun RestrictedCoachScreen(unknown: Boolean, preview: Boolean, onExitPreview: () -> Unit) {
    Column(
        Modifier.fillMaxSize().testTag("restricted-screen")
            .verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        if (preview) SampleLabel()
        CoachPanel(Modifier.fillMaxWidth()) {
            CoachIcon(CoachSymbol.SHIELD, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(stringResource(if (unknown) R.string.unknown_title else R.string.restricted_title),
                style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(if (unknown) R.string.unknown_body else R.string.restricted_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                overflow = TextOverflow.Ellipsis)
            if (!unknown) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.restricted_tip), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (preview) {
            LiquidAction(stringResource(R.string.exit_driving_preview), onExitPreview,
                Modifier.fillMaxWidth().testTag("exit-driving-preview"), CoachSymbol.BACK, secondary = true)
        }
    }
}
