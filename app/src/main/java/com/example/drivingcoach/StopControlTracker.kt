package com.example.drivingcoach

internal const val MIN_COMPARABLE_STOPS = 3

internal enum class StopSpeedBand(val label: String) {
    Low("15–30 km/h"),
    Medium("30–60 km/h");

    companion object {
        fun from(metersPerSecond: Float): StopSpeedBand? {
            val kilometersPerHour = metersPerSecond * 3.6
            return when {
                kilometersPerHour < 15.0 - SPEED_EPSILON_KMH -> null
                kilometersPerHour < 30.0 - SPEED_EPSILON_KMH -> Low
                kilometersPerHour <= 60.0 + SPEED_EPSILON_KMH -> Medium
                else -> null
            }
        }

        private const val SPEED_EPSILON_KMH = 0.001
    }
}

internal data class StopControlObservation(
    val speedBand: StopSpeedBand,
    val observedStops: Int,
    val stopsWithStrongCandidate: Int
) {
    val stopsWithoutStrongCandidate: Int
        get() = observedStops - stopsWithStrongCandidate
}

/** Groups measured forward approaches into comparable stop observations. */
internal class StopControlTracker {
    private class Approach(val band: StopSpeedBand, val startedAtNanos: Long) {
        val strongEvents = StrongDecelerationDetector()
        var hasStrongCandidate = false
        var movingIntervals = 0
        var nearZeroSinceNanos: Long? = null
    }

    private var approach: Approach? = null
    private var mayStartApproach = true
    private val observedStops = IntArray(StopSpeedBand.entries.size)
    private val stopsWithCandidate = IntArray(StopSpeedBand.entries.size)

    fun add(previous: SpeedSample, current: SpeedSample, deceleration: Double) {
        val intervalNanos = current.timestampNanos - previous.timestampNanos
        if (intervalNanos > MAX_INTERVAL_NANOS) {
            discardApproach()
            return
        }
        val resumedMotion = previous.metersPerSecond <= STOP_SPEED && current.metersPerSecond > STOP_SPEED
        if (deceleration < -RESTART_ACCELERATION || resumedMotion) {
            approach = null
            mayStartApproach = true
            return
        }

        val active = approach ?: startApproach(previous, deceleration) ?: return
        if (current.timestampNanos - active.startedAtNanos > MAX_APPROACH_NANOS) {
            discardApproach()
            return
        }
        if (previous.metersPerSecond > STOP_SPEED) active.movingIntervals++
        if (!active.hasStrongCandidate) {
            active.hasStrongCandidate = active.strongEvents.add(deceleration, intervalNanos)
        }

        if (current.metersPerSecond > STOP_SPEED) return
        val nearZeroSince = active.nearZeroSinceNanos ?: current.timestampNanos
        active.nearZeroSinceNanos = nearZeroSince
        if (current.timestampNanos - nearZeroSince < STOP_HOLD_NANOS) return

        if (active.movingIntervals >= MIN_MOVING_INTERVALS &&
            nearZeroSince - active.startedAtNanos >= MIN_APPROACH_NANOS
        ) {
            val index = active.band.ordinal
            observedStops[index]++
            if (active.hasStrongCandidate) stopsWithCandidate[index]++
        }
        discardApproach()
    }

    fun discardApproach() {
        approach = null
        mayStartApproach = false
    }

    fun observations(): List<StopControlObservation> = StopSpeedBand.entries.mapNotNull { band ->
        val count = observedStops[band.ordinal]
        if (count == 0) null else StopControlObservation(band, count, stopsWithCandidate[band.ordinal])
    }

    private fun startApproach(previous: SpeedSample, deceleration: Double): Approach? {
        if (deceleration <= 0.0) mayStartApproach = true
        if (!mayStartApproach || deceleration <= DECELERATION_FLOOR) return null

        mayStartApproach = false
        val band = StopSpeedBand.from(previous.metersPerSecond) ?: return null
        return Approach(band, previous.timestampNanos).also { approach = it }
    }

    private companion object {
        const val MAX_INTERVAL_NANOS = 1_200_000_000L
        const val MIN_APPROACH_NANOS = 1_000_000_000L
        const val MAX_APPROACH_NANOS = 20_000_000_000L
        const val STOP_HOLD_NANOS = 1_000_000_000L
        const val STOP_SPEED = 0.1f
        const val MIN_MOVING_INTERVALS = 3
        const val RESTART_ACCELERATION = 0.4
    }
}
