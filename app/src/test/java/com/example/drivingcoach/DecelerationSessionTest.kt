package com.example.drivingcoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecelerationSessionTest {
    @Test
    fun smoothAndAbruptReplayProduceDifferentResults() {
        val smooth = replay(DemoScenario.Smooth)
        val abrupt = replay(DemoScenario.Abrupt)
        assertEquals(100, smooth.score)
        assertEquals(25, abrupt.score)
        assertTrue(abrupt.peakDeceleration > smooth.peakDeceleration)
    }

    @Test
    fun scoreIsIndependentOfSamplingRateForTheSameMotion() {
        val scores = listOf(1, 5).map { samplesPerSecond ->
            val session = DecelerationSession()
            for (step in 0..10 * samplesPerSecond) {
                val seconds = step.toDouble() / samplesPerSecond
                val speed = if (seconds <= 5) 30 - seconds else 25 - (seconds - 5) * 4
                session.add(SpeedSample(
                    1_000_000_000L + step * 1_000_000_000L / samplesPerSecond,
                    speed.toFloat()
                ))
            }
            session.finish().score
        }
        assertEquals(listOf(50, 50), scores)
    }

    @Test
    fun interruptedOrMissingDataCannotReceiveAScore() {
        val interrupted = DecelerationSession()
        DemoScenario.Smooth.speeds.indices.forEach { interrupted.add(DemoScenario.Smooth.sample(it)) }
        assertNull(interrupted.finish(20_000_000_000).score)
        interrupted.interrupt()
        assertNull(interrupted.finish().score)

        val gap = DecelerationSession()
        gap.add(SpeedSample(1_000_000_000, 15f))
        gap.add(SpeedSample(9_000_000_000, 0f))
        assertNull(gap.finish().score)
    }

    @Test
    fun stationaryAndReverseMotionCannotReceiveAForwardDecelerationScore() {
        val stationary = DecelerationSession()
        for (second in 1L..10L) stationary.add(SpeedSample(second * 1_000_000_000, 0f))
        assertNull(stationary.finish().score)
        stationary.add(SpeedSample(11_000_000_000, -1f))
        assertNull(stationary.finish().score)
    }

    @Test
    fun duplicateAndOlderEventsCannotCreateAFalseDeceleration() {
        val session = DecelerationSession()
        DemoScenario.Smooth.speeds.indices.forEach {
            val sample = DemoScenario.Smooth.sample(it)
            session.add(sample)
            session.add(SpeedSample(sample.timestampNanos, 0f))
            session.add(SpeedSample(sample.timestampNanos - 1, 0f))
        }
        assertEquals(100, session.finish().score)
    }

    private fun replay(scenario: DemoScenario): DecelerationResult {
        val session = DecelerationSession()
        scenario.speeds.indices.forEach { session.add(scenario.sample(it)) }
        return session.finish()
    }
}
