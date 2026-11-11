package com.example.drivingcoach.ui.model

/** Presentation data only. These types do not define a server payload or scoring policy. */
data class DriverOverview(
    val origin: OverviewOrigin,
    val score: DrivingScoreSummary,
    val habits: List<DrivingHabitAnalysis>,
    val progress: DriverProgress = DriverProgress.Unassigned,
) {
    init {
        require(progress !is DriverProgress.Preview || origin == OverviewOrigin.MOCK) {
            "Provisional tier examples must remain mock data"
        }
    }
}

enum class OverviewOrigin { MOCK, RECORDED }

/** A display value on the existing 0..100 scale, never calculated from practice counts. */
data class DrivingScore(val value: Int) {
    init {
        require(value in 0..100) { "Display scores must be in 0..100" }
    }
}

data class DrivingScoreSummary(
    val current: DrivingScore?,
    val previous: DrivingScore? = null,
    val trend: List<ScoreObservation> = emptyList(),
) {
    /** Missing measurements are not zero scores and cannot produce a comparison. */
    val change: Int?
        get() = current?.let { currentScore -> previous?.let { currentScore.value - it.value } }
}

data class ScoreObservation(val completedAtMillis: Long, val score: DrivingScore?)

data class DrivingHabitAnalysis(
    val kind: HabitKind,
    val status: HabitStatus,
    val observations: ObservationCount? = null,
)

enum class HabitKind { STOP_CONTROL, SIGNAL_TIMING, SIGNAL_HOLD, SIGNAL_CANCEL }

enum class HabitStatus { MET, REVIEW, UNOBSERVED }

/** Retains the existing practice evaluator's met/observed meaning without inventing a score. */
data class ObservationCount(val met: Int, val observed: Int) {
    init {
        require(observed >= 0 && met in 0..observed) { "Counts must satisfy 0 <= met <= observed" }
    }

    val hasObservations: Boolean get() = observed > 0
}

sealed interface DriverProgress {
    /** Default until product rules and a trusted progress source exist. */
    data object Unassigned : DriverProgress

    /** Only a layout fixture: labels, levels and experience do not establish real tier rules. */
    data class Preview(
        val level: Int,
        val tierLabel: String,
        val earnedWithinTier: Long,
        val requiredWithinTier: Long?,
    ) : DriverProgress {
        init {
            require(level > 0 && tierLabel.isNotBlank() && earnedWithinTier >= 0)
            require(requiredWithinTier == null || requiredWithinTier > 0)
        }

        val isHighestTier: Boolean get() = requiredWithinTier == null

        /** null represents the highest example tier, which has no next-tier target. */
        val fraction: Float?
            get() = requiredWithinTier?.let {
                (earnedWithinTier.toDouble() / it.toDouble()).coerceIn(0.0, 1.0).toFloat()
            }
    }
}
