package com.example.drivingcoach.data.mock

import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.OverviewFailure
import com.example.drivingcoach.ui.model.DriverOverview
import com.example.drivingcoach.ui.model.DriverProgress
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.DrivingScore
import com.example.drivingcoach.ui.model.DrivingScoreSummary
import com.example.drivingcoach.ui.model.HabitKind
import com.example.drivingcoach.ui.model.HabitStatus
import com.example.drivingcoach.ui.model.ObservationCount
import com.example.drivingcoach.ui.model.OverviewOrigin
import com.example.drivingcoach.ui.model.ScoreObservation

enum class OverviewScenario { NORMAL, LOADING, EMPTY, ERROR, PARTIAL, TIER_EXAMPLE, HIGHEST_TIER }

/** Fixtures for frontend development; none of the values represent a driver's real assessment. */
object MockDriverOverviewData {
    fun state(scenario: OverviewScenario): DriverOverviewState = when (scenario) {
        OverviewScenario.LOADING -> DriverOverviewState.Loading
        OverviewScenario.EMPTY -> DriverOverviewState.Empty
        OverviewScenario.ERROR -> DriverOverviewState.Error(OverviewFailure.UNAVAILABLE)
        else -> DriverOverviewState.Ready(overview(scenario))
    }

    private fun overview(scenario: OverviewScenario): DriverOverview {
        val partial = scenario == OverviewScenario.PARTIAL
        return DriverOverview(
            origin = OverviewOrigin.MOCK,
            score = DrivingScoreSummary(
                current = if (partial) null else DrivingScore(92),
                previous = DrivingScore(88),
                trend = listOf(40L to 72, 29L to 75, 20L to 78, 12L to 81, 8L to 84,
                    5L to 86, 3L to 87, 1L to 88, 0L to 92).map { (daysAgo, score) ->
                    ScoreObservation(1_783_209_600_000L - daysAgo * 86_400_000L,
                        if (partial && daysAgo in listOf(3L, 0L)) null else DrivingScore(score))
                },
            ),
            habits = listOf(
                DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(2, 3)),
                DrivingHabitAnalysis(HabitKind.SIGNAL_TIMING, HabitStatus.MET, ObservationCount(3, 3)),
                DrivingHabitAnalysis(HabitKind.SIGNAL_HOLD,
                    if (partial) HabitStatus.UNOBSERVED else HabitStatus.MET,
                    if (partial) null else ObservationCount(3, 3)),
                DrivingHabitAnalysis(HabitKind.SIGNAL_CANCEL,
                    if (partial) HabitStatus.UNOBSERVED else HabitStatus.REVIEW,
                    if (partial) ObservationCount(0, 0) else ObservationCount(2, 3)),
            ),
            progress = when (scenario) {
                OverviewScenario.TIER_EXAMPLE -> DriverProgress.Preview(3, "A", 60, 100)
                OverviewScenario.HIGHEST_TIER -> DriverProgress.Preview(5, "Z", 100, null)
                else -> DriverProgress.Unassigned
            },
        )
    }
}
