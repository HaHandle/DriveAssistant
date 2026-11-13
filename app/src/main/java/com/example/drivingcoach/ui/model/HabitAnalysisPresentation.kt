package com.example.drivingcoach.ui.model

/** Facts about supplied counts, independent of the evaluator's reported status. */
enum class HabitEvidenceState { NOT_PROVIDED, NO_OBSERVATIONS, NONE_MET, HAS_MET }

/** A display decision, not a replacement evaluation or a scoring policy. */
enum class HabitAssessmentState { MET, REVIEW, DEFERRED }

@ConsistentCopyVisibility
data class HabitAnalysisPresentation internal constructor(
    val kind: HabitKind,
    val reportedStatus: HabitStatus,
    val observations: ObservationCount?,
) {
    val evidence: HabitEvidenceState = when {
        observations == null -> HabitEvidenceState.NOT_PROVIDED
        !observations.hasObservations -> HabitEvidenceState.NO_OBSERVATIONS
        observations.met == 0 -> HabitEvidenceState.NONE_MET
        else -> HabitEvidenceState.HAS_MET
    }

    val assessment: HabitAssessmentState = when {
        observations == null || !observations.hasObservations -> HabitAssessmentState.DEFERRED
        reportedStatus == HabitStatus.UNOBSERVED -> HabitAssessmentState.DEFERRED
        reportedStatus == HabitStatus.REVIEW -> HabitAssessmentState.REVIEW
        // Keep an unsupported success claim deferred; retain the original status and counts above.
        observations.met == 0 -> HabitAssessmentState.DEFERRED
        else -> HabitAssessmentState.MET
    }

    /** Never display 0/0. Positive observation counts remain available even if assessment is deferred. */
    val countsForDisplay: ObservationCount? = observations?.takeIf { it.hasObservations }
}

/** Preserve each independent habit and its source values without deriving a score or pass threshold. */
fun DrivingHabitAnalysis.toHabitAnalysisPresentation(): HabitAnalysisPresentation =
    HabitAnalysisPresentation(kind, status, observations)
