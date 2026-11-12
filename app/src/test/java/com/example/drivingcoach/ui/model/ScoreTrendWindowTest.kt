package com.example.drivingcoach.ui.model

import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreTrendWindowTest {
    private val utc = ZoneId.of("UTC")
    private fun point(at: String, score: Int?) =
        ScoreObservation(Instant.parse(at).toEpochMilli(), score?.let(::DrivingScore))

    @Test fun weekIncludesSevenCalendarDatesAndExcludesThePreviousDay() {
        val before = point("2026-11-03T23:59:59.999Z", 70)
        val first = point("2026-11-04T00:00:00Z", 80)
        val last = point("2026-11-10T12:00:00Z", 90)
        val window = buildScoreTrendWindow(listOf(before, first, last), ScorePeriod.WEEK, utc)
        assertEquals(listOf(first, last), window.points)
        assertEquals(LocalDate.of(2026, 11, 4), window.startDate)
        assertEquals(LocalDate.of(2026, 11, 10), window.endDate)
    }

    @Test fun monthMeansThirtyInclusiveDatesAcrossTheMonthBoundary() {
        val before = point("2026-10-11T23:59:59Z", 70)
        val first = point("2026-10-12T00:00:00Z", 80)
        val last = point("2026-11-10T12:00:00Z", 90)
        assertEquals(listOf(first, last),
            buildScoreTrendWindow(listOf(before, first, last), ScorePeriod.MONTH, utc).points)
        assertEquals(listOf(before, first, last),
            buildScoreTrendWindow(listOf(before, first, last), ScorePeriod.ALL, utc).points)
    }

    @Test fun sortingDoesNotMutateOrRetainTheCallerList() {
        val first = point("2026-10-01T12:00:00Z", 80)
        val last = point("2026-11-10T12:00:00Z", 90)
        val input = mutableListOf(last, first)
        val window = buildScoreTrendWindow(input, ScorePeriod.ALL, utc)
        assertEquals(listOf(last, first), input)
        input.clear()
        assertEquals(listOf(first, last), window.points)
    }

    @Test fun theLatestMissingRecordStillAnchorsTheSelectedPeriod() {
        val older = point("2026-01-01T12:00:00Z", 80)
        val missing = point("2026-02-10T12:00:00Z", null)
        val window = buildScoreTrendWindow(listOf(older, missing), ScorePeriod.WEEK, utc)
        assertEquals(listOf(missing), window.points)
        assertEquals(LocalDate.of(2026, 2, 10), window.endDate)
        assertEquals(0, window.measuredCount)
        assertEquals(1, window.missingCount)
        assertNull(window.change)
    }

    @Test fun comparisonUsesPeriodEndpointsRatherThanAnAverageOrThePreviousRecord() {
        val window = buildScoreTrendWindow(listOf(
            point("2026-11-01T12:00:00Z", 80),
            point("2026-11-05T12:00:00Z", 99),
            point("2026-11-10T12:00:00Z", 85)), ScorePeriod.ALL, utc)
        assertEquals(5, window.change)
    }

    @Test fun aMissingEndpointCannotBeSkippedToProduceAComparison() {
        for (scores in listOf(listOf(null, 80, 90), listOf(80, 90, null))) {
            val points = scores.mapIndexed { index, value -> point("2026-11-0${index + 1}T12:00:00Z", value) }
            assertNull(buildScoreTrendWindow(points, ScorePeriod.ALL, utc).change)
        }
    }

    @Test fun missingMeasurementsBreakTheLineWithoutBecomingZero() {
        val first = point("2026-11-01T12:00:00Z", 80)
        val missing = point("2026-11-02T12:00:00Z", null)
        val last = point("2026-11-03T12:00:00Z", 90)
        val window = buildScoreTrendWindow(listOf(first, missing, last), ScorePeriod.ALL, utc)
        assertEquals(listOf(listOf(first), listOf(last)), window.segments)
        assertEquals(2, window.measuredCount)
        assertEquals(1, window.missingCount)
        assertEquals(10, window.change)
    }

    @Test fun measuredZeroAndTheFullScoreRangeRemainValid() {
        val first = point("2026-11-01T12:00:00Z", 100)
        val last = point("2026-11-02T12:00:00Z", 0)
        val window = buildScoreTrendWindow(listOf(first, last), ScorePeriod.ALL, utc)
        assertEquals(-100, window.change)
        assertEquals(2, window.measuredCount)
        assertEquals(0, window.missingCount)
        assertEquals(listOf(listOf(first, last)), window.segments)
    }

    @Test fun emptyHistoryHasNoInventedDatesScoresOrComparison() {
        val window = buildScoreTrendWindow(emptyList(), ScorePeriod.WEEK, utc)
        assertEquals(emptyList<ScoreObservation>(), window.points)
        assertEquals(0, window.measuredCount)
        assertNull(window.change)
        assertNull(window.startDate)
        assertNull(window.endDate)
        assertEquals(.5f, window.timeFraction(0), 0f)
    }

    @Test fun oneRecordAtMidnightHasAFiniteCenteredPositionAndNoComparison() {
        val only = point("2026-11-01T00:00:00Z", 0)
        val window = buildScoreTrendWindow(listOf(only), ScorePeriod.ALL, utc)
        assertNull(window.change)
        assertEquals(.5f, window.timeFraction(only.completedAtMillis), 0f)
        assertEquals(listOf(listOf(only)), window.segments)
    }

    @Test fun equalTimestampsDoNotInventADirectionOrAConnectingLine() {
        val first = point("2026-11-01T12:00:00Z", 80)
        val second = point("2026-11-01T12:00:00Z", 90)
        val window = buildScoreTrendWindow(listOf(first, second), ScorePeriod.ALL, utc)
        assertNull(window.change)
        assertEquals(listOf(first, second), window.points)
        assertEquals(listOf(listOf(first), listOf(second)), window.segments)
    }

    @Test fun horizontalPositionsUseElapsedTimeInsteadOfEvenRecordSpacing() {
        val points = listOf(point("2026-11-01T00:00:00Z", 80),
            point("2026-11-01T02:00:00Z", 85), point("2026-11-01T10:00:00Z", 90))
        val window = buildScoreTrendWindow(points, ScorePeriod.ALL, utc)
        assertEquals(0f, window.timeFraction(points.first().completedAtMillis), .0001f)
        assertEquals(.2f, window.timeFraction(points[1].completedAtMillis), .0001f)
        assertEquals(1f, window.timeFraction(points.last().completedAtMillis), .0001f)
    }

    @Test fun daylightSavingChangesDoNotTurnCalendarDaysIntoFixedHourWindows() {
        val before = point("2026-11-01T03:59:59Z", 70)
        val first = point("2026-11-01T04:00:00Z", 80)
        val last = point("2026-11-07T17:00:00Z", 90)
        val window = buildScoreTrendWindow(listOf(before, first, last), ScorePeriod.WEEK,
            ZoneId.of("America/New_York"))
        assertEquals(listOf(first, last), window.points)
        assertEquals(LocalDate.of(2026, 11, 1), window.startDate)
        assertEquals(157L, (window.referenceAtMillis!! - window.startAtMillis!!) / 3_600_000L)
    }

    @Test fun calendarBoundariesFollowTheProvidedTimeZone() {
        val before = point("2026-11-03T14:59:59Z", 70)
        val first = point("2026-11-03T15:00:00Z", 80)
        val last = point("2026-11-09T15:30:00Z", 90)
        val window = buildScoreTrendWindow(listOf(before, first, last), ScorePeriod.WEEK,
            ZoneId.of("Asia/Seoul"))
        assertEquals(listOf(first, last), window.points)
        assertEquals(LocalDate.of(2026, 11, 4), window.startDate)
        assertEquals(LocalDate.of(2026, 11, 10), window.endDate)
    }

    @Test fun mockPeriodsProduceDifferentRecordsAndComparisonsWithoutChangingTheHeadlineScore() {
        val overview = (MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready).overview
        val windows = ScorePeriod.entries.map { buildScoreTrendWindow(overview.score.trend, it, utc) }
        assertEquals(listOf(4, 8, 9), windows.map { it.points.size })
        assertEquals(listOf(6, 17, 20), windows.map { it.change })
        assertEquals(92, overview.score.current?.value)
        assertEquals(4, overview.score.change)
    }
}
