package com.example.drivingcoach.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverOverviewTest {
    @Test
    fun zeroScoreRemainsMeasuredAndMissingScoreCannotProduceAComparison() {
        val measured = DrivingScoreSummary(DrivingScore(0), DrivingScore(20))
        assertEquals(0, measured.current?.value)
        assertEquals(-20, measured.change)
        assertNull(DrivingScoreSummary(null, DrivingScore(20)).change)
        assertNull(DrivingScoreSummary(DrivingScore(20), null).change)
    }

    @Test
    fun fullScaleComparisonsPreserveTheirDirection() {
        assertEquals(100, DrivingScoreSummary(DrivingScore(100), DrivingScore(0)).change)
        assertEquals(-100, DrivingScoreSummary(DrivingScore(0), DrivingScore(100)).change)
        assertEquals(0, DrivingScoreSummary(DrivingScore(92), DrivingScore(92)).change)
    }

    @Test
    fun scoreOutsideTheDisplayScaleIsRejectedInsteadOfSilentlyClamped() {
        assertTrue(runCatching { DrivingScore(-1) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { DrivingScore(101) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun noObservationsAndAnObservedUnmetGoalRemainDifferent() {
        assertFalse(ObservationCount(0, 0).hasObservations)
        assertTrue(ObservationCount(0, 3).hasObservations)
        assertTrue(runCatching { ObservationCount(4, 3) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { ObservationCount(-1, 3) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun unassignedProgressIsTheDefaultForRecordedData() {
        val overview = DriverOverview(OverviewOrigin.RECORDED, DrivingScoreSummary(null), emptyList())
        assertEquals(DriverProgress.Unassigned, overview.progress)
    }

    @Test
    fun provisionalTierCannotBePresentedAsARecordedRating() {
        val preview = DriverProgress.Preview(3, "A", 60, 100)
        assertTrue(runCatching {
            DriverOverview(OverviewOrigin.RECORDED, DrivingScoreSummary(null), emptyList(), preview)
        }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun tierPreviewProgressHandlesStartBoundaryAndOverflow() {
        assertEquals(0f, DriverProgress.Preview(1, "A", 0, 100).fraction!!, 0f)
        assertEquals(.6f, DriverProgress.Preview(1, "A", 60, 100).fraction!!, .0001f)
        assertEquals(1f, DriverProgress.Preview(1, "A", 100, 100).fraction!!, 0f)
        assertEquals(1f, DriverProgress.Preview(1, "A", Long.MAX_VALUE, 100).fraction!!, 0f)
    }

    @Test
    fun highestTierHasNoNextTargetAndInvalidTargetsAreRejected() {
        val highest = DriverProgress.Preview(5, "Z", 100, null)
        assertTrue(highest.isHighestTier)
        assertNull(highest.fraction)
        assertTrue(runCatching { DriverProgress.Preview(1, "A", 0, 0) }
            .exceptionOrNull() is IllegalArgumentException)
    }
}
