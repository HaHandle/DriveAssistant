package com.example.drivingcoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopControlTrackerTest {
    @Test
    fun speedBandsHonorDocumentedBoundaries() {
        assertNull(StopSpeedBand.from(metersPerSecond(14.9)))
        assertEquals(StopSpeedBand.Low, StopSpeedBand.from(metersPerSecond(15.0)))
        assertEquals(StopSpeedBand.Low, StopSpeedBand.from(metersPerSecond(29.9)))
        assertEquals(StopSpeedBand.Medium, StopSpeedBand.from(metersPerSecond(30.0)))
        assertEquals(StopSpeedBand.Medium, StopSpeedBand.from(metersPerSecond(60.0)))
        assertNull(StopSpeedBand.from(metersPerSecond(60.1)))
    }

    @Test
    fun validStopIsCountedOnlyOnceAfterHoldDuration() {
        val tracker = StopControlTracker()
        addSpeeds(tracker, listOf(10f, 8f, 6f, 4f, 2f, 0f, 0f, 0f))

        val observation = tracker.observations().single()
        assertEquals(StopSpeedBand.Medium, observation.speedBand)
        assertEquals(1, observation.observedStops)
        assertEquals(0, observation.stopsWithStrongCandidate)
    }

    @Test
    fun shortApproachAndLongSampleGapAreDiscarded() {
        val shortApproach = StopControlTracker()
        addSpeeds(shortApproach, listOf(10f, 5f, 0f, 0f))
        assertEquals(emptyList<StopControlObservation>(), shortApproach.observations())

        val gap = StopControlTracker()
        gap.add(SpeedSample(1_000_000_000L, 10f), SpeedSample(2_300_000_001L, 8f), 1.54)
        assertEquals(emptyList<StopControlObservation>(), gap.observations())
    }

    @Test
    fun reaccelerationDiscardsApproachAndAllowsTheNextStop() {
        val tracker = StopControlTracker()

        addSpeeds(tracker, listOf(10f, 8f, 9f, 7f, 5f, 3f, 0f, 0f))

        val observation = tracker.observations().single()
        assertEquals(StopSpeedBand.Medium, observation.speedBand)
        assertEquals(1, observation.observedStops)
    }

    private fun addSpeeds(tracker: StopControlTracker, speeds: List<Float>) {
        speeds.zipWithNext().forEachIndexed { index, (previousSpeed, currentSpeed) ->
            val previous = SpeedSample((index + 1) * SECOND_NANOS, previousSpeed)
            val current = SpeedSample((index + 2) * SECOND_NANOS, currentSpeed)
            tracker.add(previous, current, (previousSpeed - currentSpeed).toDouble())
        }
    }

    private fun metersPerSecond(kilometersPerHour: Double): Float =
        (kilometersPerHour / 3.6).toFloat()

    private companion object {
        const val SECOND_NANOS = 1_000_000_000L
    }
}
