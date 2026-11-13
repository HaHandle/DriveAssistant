package com.example.drivingcoach.analysis

import kotlin.math.max
import kotlin.math.roundToInt

internal const val DECELERATION_FLOOR = 0.2

internal data class SpeedSample(val timestampNanos: Long, val metersPerSecond: Float)

internal data class DecelerationResult(
    val score: Int?,
    val feedback: String,
    val peakDeceleration: Double = 0.0,
    val strongEventCount: Int? = null,
    val stopObservations: List<StopControlObservation>? = null
)

/** Estimates forward deceleration from vehicle speed. Event thresholds are provisional. */
internal class DecelerationSession(private val startedAtNanos: Long? = null) {
    private var previous: SpeedSample? = null
    private var lastTimestampNanos: Long? = null
    private var forwardIntervals = 0
    private var decelerationNanos = 0L
    private var strongDecelerationNanos = 0L
    private var peakDeceleration = 0.0
    private var interrupted = false

    private var strongEventCount = 0
    private val strongEvents = StrongDecelerationDetector()
    private val stopControl = StopControlTracker()

    fun add(sample: SpeedSample) {
        if (interrupted) return
        val timestamp = sample.timestampNanos
        if (timestamp <= 0) {
            interrupt()
            return
        }
        if (startedAtNanos != null && timestamp < startedAtNanos) return
        val lastTimestamp = lastTimestampNanos
        if (lastTimestamp != null && timestamp <= lastTimestamp) return
        val precedingTimestamp = lastTimestamp ?: startedAtNanos
        if (!sample.metersPerSecond.isFinite() ||
            (precedingTimestamp != null && timestamp - precedingTimestamp > MAX_GAP_NANOS)
        ) {
            interrupt()
            return
        }
        lastTimestampNanos = timestamp

        val last = previous
        if (last == null) {
            previous = sample
            return
        }
        if (last.metersPerSecond < 0f || sample.metersPerSecond < 0f) {
            previous = sample
            strongEvents.reset()
            stopControl.discardApproach()
            return
        }
        // Wait for a minimum interval before taking another speed difference.
        val intervalNanos = timestamp - last.timestampNanos
        if (intervalNanos < MIN_INTERVAL_NANOS) return
        previous = sample

        if (last.metersPerSecond > 0f || sample.metersPerSecond > 0f) forwardIntervals++
        val seconds = intervalNanos / 1_000_000_000.0
        val deceleration = (last.metersPerSecond.toDouble() - sample.metersPerSecond) / seconds
        peakDeceleration = max(peakDeceleration, deceleration)
        if (deceleration > DECELERATION_FLOOR) decelerationNanos += intervalNanos
        if (deceleration > StrongDecelerationDetector.START_THRESHOLD) {
            strongDecelerationNanos += intervalNanos
        }
        if (strongEvents.add(deceleration, intervalNanos)) strongEventCount++
        stopControl.add(last, sample, deceleration)
    }

    fun interrupt() {
        interrupted = true
    }

    fun finish(endTimestampNanos: Long? = null, includeDemoScore: Boolean = true): DecelerationResult {
        val lastTimestamp = lastTimestampNanos
        val stale = endTimestampNanos != null && lastTimestamp != null &&
            endTimestampNanos - lastTimestamp !in 0L..MAX_GAP_NANOS
        if (interrupted || stale) {
            return DecelerationResult(null, "연수 기록이 중단되어 결과를 확인할 수 없어요.")
        }
        val stops = stopControl.observations()
        if (forwardIntervals < MIN_FORWARD_INTERVALS || decelerationNanos < MIN_DECELERATION_NANOS) {
            return DecelerationResult(null, "관찰한 감속 구간이 충분하지 않아요.", stopObservations = stops)
        }
        val score = if (includeDemoScore) {
            (100 * (1 - strongDecelerationNanos.toDouble() / decelerationNanos)).roundToInt()
        } else null
        val feedback = if (strongEventCount > 0) {
            "속도가 빠르게 줄었던 당시의 교통 상황을 떠올려 보세요."
        } else {
            "측정 구간에서 강한 감속 기준을 충족한 사건이 없어요."
        }
        return DecelerationResult(score, feedback, peakDeceleration, strongEventCount, stops)
    }

    private companion object {
        const val MIN_INTERVAL_NANOS = 100_000_000L
        const val MAX_GAP_NANOS = 2_000_000_000L
        const val MIN_FORWARD_INTERVALS = 4
        const val MIN_DECELERATION_NANOS = 3_000_000_000L
    }
}

internal class StrongDecelerationDetector {
    private var eventActive = false
    private var aboveThresholdNanos = 0L
    private var belowThresholdNanos = 0L

    /** Returns true when the current sequence first qualifies as a strong event. */
    fun add(deceleration: Double, intervalNanos: Long): Boolean {
        if (eventActive) {
            belowThresholdNanos = if (deceleration <= END_THRESHOLD) belowThresholdNanos + intervalNanos else 0L
            if (belowThresholdNanos >= MIN_RELEASE_NANOS) reset()
            return false
        }
        aboveThresholdNanos = if (deceleration > START_THRESHOLD) aboveThresholdNanos + intervalNanos else 0L
        eventActive = aboveThresholdNanos >= MIN_EVENT_NANOS
        return eventActive
    }

    fun reset() {
        eventActive = false
        aboveThresholdNanos = 0L
        belowThresholdNanos = 0L
    }

    companion object {
        const val START_THRESHOLD = 3.0
        private const val END_THRESHOLD = 2.0
        private const val MIN_EVENT_NANOS = 600_000_000L
        private const val MIN_RELEASE_NANOS = 300_000_000L
    }
}

internal enum class DemoScenario(
    val label: String,
    val speeds: List<Float>,
    val replayDelayMillis: Long = 1_000L
) {
    Smooth("부드러운 감속", listOf(15f, 13.5f, 12f, 10.5f, 9f, 7.5f, 6f, 4.5f, 3f, 1.5f, 0f, 0f)),
    Abrupt("급한 감속", listOf(15f, 15f, 15f, 11f, 7f, 3f, 0f, 0f, 0f, 0f, 0f)),
    Repeated(
        "반복 정차",
        listOf(
            15f, 12.5f, 10f, 7.5f, 5f, 2.5f, 0f, 0f,
            3f, 6f, 9f, 12f, 15f, 11f, 7f, 3f, 0f, 0f,
            3f, 6f, 9f, 12f, 15f, 12.5f, 10f, 7.5f, 5f, 2.5f, 0f, 0f
        ),
        replayDelayMillis = 400L
    );

    fun next() = entries[(ordinal + 1) % entries.size]

    fun sample(index: Int) = SpeedSample((index + 1) * 1_000_000_000L, speeds[index])
}
