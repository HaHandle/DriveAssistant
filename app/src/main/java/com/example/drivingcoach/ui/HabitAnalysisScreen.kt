package com.example.drivingcoach.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.R
import com.example.drivingcoach.ui.model.HabitAnalysisPresentation
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.findHabitDetail
import com.example.drivingcoach.ui.model.toHabitAnalysisPresentation

/** Invalid data and restrictions bypass the transition, removing outgoing details in the same frame. */
@Composable
internal fun HabitAnalysisDestination(
    selectedKindName: String?,
    habits: List<DrivingHabitAnalysis>,
    enabled: Boolean,
    onBack: () -> Unit,
    home: @Composable () -> Unit,
) {
    if (!enabled || (selectedKindName != null && findHabitDetail(habits, selectedKindName) == null)) {
        home()
        return
    }
    val motion = LocalCoachMotion.current
    val detailState = rememberSaveableStateHolder()
    @Composable
    fun Destination(key: String?) {
        if (key == null) home()
        else findHabitDetail(habits, key)?.let { habit ->
            detailState.SaveableStateProvider(key) {
                HabitAnalysisScreen(habit.toHabitAnalysisPresentation(), onBack)
            }
        }
    }
    // Bypass an already-running transition too, rather than waiting for its old fade to finish.
    if (!motion) {
        Destination(selectedKindName)
        return
    }
    AnimatedContent(
        targetState = selectedKindName,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(tween(180)) togetherWith fadeOut(tween(100))
        },
        label = "habitDestination",
    ) { key ->
        Destination(key)
    }
}

/** Stateless detail content. The app gates navigation and data before composing this screen. */
@Composable
fun HabitAnalysisScreen(habit: HabitAnalysisPresentation, onBack: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("habit-detail")) {
        val wide = maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Eyebrow(stringResource(R.string.habit_detail_eyebrow))
            Text(stringResource(habitTitle(habit.kind)), Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge)
            LiquidAction(stringResource(R.string.habit_back), onBack,
                Modifier.fillMaxWidth().testTag("habit-back"), symbol = CoachSymbol.BACK, secondary = true)
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AssessmentPanel(habit, Modifier.weight(1f))
                    EvidencePanel(habit, Modifier.weight(1f))
                }
            } else {
                AssessmentPanel(habit, Modifier.fillMaxWidth())
                EvidencePanel(habit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AssessmentPanel(habit: HabitAnalysisPresentation, modifier: Modifier) {
    CoachPanel(modifier) {
        HabitSectionHeading(R.string.habit_assessment_heading)
        HabitAssessmentContent(habit, explain = true)
    }
}

@Composable
private fun EvidencePanel(habit: HabitAnalysisPresentation, modifier: Modifier) {
    CoachPanel(modifier) {
        HabitSectionHeading(R.string.habit_evidence_heading)
        HabitEvidenceContent(habit)
    }
}
