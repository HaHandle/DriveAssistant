package com.example.drivingcoach.analysis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrongDecelerationDetectorTest {
    @Test
    fun eventRequiresStrictThresholdAndMinimumDuration() {
        val detector = StrongDecelerationDetector()

        assertFalse(detector.add(3.0, 1_000_000_000L))
        assertFalse(detector.add(3.1, 599_000_000L))
        assertTrue(detector.add(3.1, 1_000_000L))
        assertFalse(detector.add(4.0, 1_000_000_000L))
    }

    @Test
    fun eventRearmsOnlyAfterRecoveryDuration() {
        val detector = StrongDecelerationDetector()

        assertTrue(detector.add(3.1, 600_000_000L))
        assertFalse(detector.add(2.0, 299_000_000L))
        assertFalse(detector.add(2.0, 1_000_000L))
        assertFalse(detector.add(3.1, 599_000_000L))
        assertTrue(detector.add(3.1, 1_000_000L))
    }

    @Test
    fun interruptedCandidateDurationDoesNotCarryForward() {
        val detector = StrongDecelerationDetector()

        assertFalse(detector.add(3.1, 500_000_000L))
        assertFalse(detector.add(3.0, 100_000_000L))
        assertFalse(detector.add(3.1, 500_000_000L))
        assertTrue(detector.add(3.1, 100_000_000L))
    }
}
