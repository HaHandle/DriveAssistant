package com.example.drivingcoach.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.R
import com.example.drivingcoach.ui.model.HabitAnalysisPresentation
import com.example.drivingcoach.ui.model.HabitAssessmentState
import com.example.drivingcoach.ui.model.HabitEvidenceState
import com.example.drivingcoach.ui.model.HabitKind

@StringRes
internal fun habitTitle(kind: HabitKind): Int = when (kind) {
    HabitKind.STOP_CONTROL -> R.string.habit_stop_control
    HabitKind.SIGNAL_TIMING -> R.string.habit_signal_timing
    HabitKind.SIGNAL_HOLD -> R.string.habit_signal_hold
    HabitKind.SIGNAL_CANCEL -> R.string.habit_signal_cancel
}

@Composable
internal fun HabitSummaryRow(habit: HabitAnalysisPresentation, onClick: (() -> Unit)?) {
    val title = stringResource(habitTitle(habit.kind))
    val openLabel = stringResource(R.string.habit_open_details, title)
    val interaction = if (onClick == null) Modifier else Modifier.clickable(
        role = Role.Button, onClickLabel = openLabel, onClick = onClick)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).then(interaction)
            .testTag("habit-row-${habit.kind.name}").padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (onClick != null) CoachIcon(CoachSymbol.ARROW)
        }
        HabitAssessmentContent(habit)
        HabitEvidenceContent(habit)
    }
}

@Composable
internal fun HabitAssessmentContent(habit: HabitAnalysisPresentation, explain: Boolean = false) {
    StatusPill(stringResource(when (habit.assessment) {
        HabitAssessmentState.MET -> R.string.habit_met
        HabitAssessmentState.REVIEW -> R.string.habit_review
        HabitAssessmentState.DEFERRED -> R.string.habit_deferred
    }))
    if (explain && habit.assessment == HabitAssessmentState.DEFERRED) {
        Text(stringResource(when (habit.evidence) {
            HabitEvidenceState.NOT_PROVIDED -> R.string.habit_reason_missing
            HabitEvidenceState.NO_OBSERVATIONS -> R.string.habit_reason_unobserved
            else -> R.string.habit_reason_pending
        }), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun HabitEvidenceContent(habit: HabitAnalysisPresentation) {
    Text(stringResource(when (habit.evidence) {
        HabitEvidenceState.NOT_PROVIDED -> R.string.habit_evidence_missing
        HabitEvidenceState.NO_OBSERVATIONS -> R.string.habit_evidence_unobserved
        HabitEvidenceState.NONE_MET -> R.string.habit_evidence_none_met
        HabitEvidenceState.HAS_MET -> R.string.habit_evidence_has_met
    }), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    habit.countsForDisplay?.let { counts ->
        Text(stringResource(R.string.habit_observations, counts.met, counts.observed),
            style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun HabitSectionHeading(@StringRes title: Int) {
    Text(stringResource(title), Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium)
}
