package com.example.drivingcoach

import kotlin.math.max
import kotlin.math.roundToInt

internal data class SpeedSample(val timestampNanos: Long, val metersPerSecond: Float)

internal data class DecelerationResult(
    val score: Int?,
    val feedback: String,
    val peakDeceleration: Double = 0.0
)

/** Experimental comfort metric. Thresholds require calibration against recorded vehicle data. */
internal class DecelerationSession {
    private var previous: SpeedSample? = null
    private var sampleCount = 0
    private var decelerationSeconds = 0.0
    private var strongDecelerationSeconds = 0.0
    private var peakDeceleration = 0.0
    private var interrupted = false

    fun add(sample: SpeedSample) {
        if (!sample.metersPerSecond.isFinite() || sample.metersPerSecond < 0 ||
            sample.timestampNanos <= 0
        ) {
            interrupt()
            return
        }
        val last = previous
        if (last != null) {
            val seconds = (sample.timestampNanos - last.timestampNanos) / 1_000_000_000.0
            if (seconds <= 0) return
            if (seconds > 2.0) {
                interrupted = true
            } else {
                val deceleration = (last.metersPerSecond - sample.metersPerSecond) / seconds
                peakDeceleration = max(peakDeceleration, deceleration)
                if (deceleration > 0.2) {
                    decelerationSeconds += seconds
                    if (deceleration > 3.0) strongDecelerationSeconds += seconds
                }
            }
        }
        previous = sample
        sampleCount++
    }

    fun interrupt() {
        interrupted = true
        previous = null
    }

    fun finish(endTimestampNanos: Long? = null): DecelerationResult {
        val lastTimestamp = previous?.timestampNanos
        val stale = endTimestampNanos != null && lastTimestamp != null &&
            endTimestampNanos - lastTimestamp > 2_000_000_000L
        if (interrupted || stale) {
            return DecelerationResult(null, "데이터 수신이 중단되어 평가를 완료하지 못했어요. 다시 연습해 주세요.")
        }
        if (sampleCount < 5 || decelerationSeconds < 3.0) {
            return DecelerationResult(null, "평가할 감속 데이터가 부족해요. 충분한 데이터를 수집한 뒤 다시 확인해 주세요.")
        }
        val score = (100 * (1 - strongDecelerationSeconds / decelerationSeconds)).roundToInt()
        val feedback = if (strongDecelerationSeconds > 0) {
            "속도가 빠르게 줄어든 구간이 있었어요. 주차 후 해당 상황을 복기해 보세요."
        } else {
            "수집된 감속 구간에서는 속도가 비교적 부드럽게 줄었어요."
        }
        return DecelerationResult(score.coerceIn(0, 100), feedback, peakDeceleration)
    }
}

internal enum class DemoScenario(val label: String, val speeds: List<Float>) {
    Smooth("부드러운 감속", listOf(15f, 13.5f, 12f, 10.5f, 9f, 7.5f, 6f, 4.5f, 3f, 1.5f, 0f)),
    Abrupt("급한 감속", listOf(15f, 15f, 15f, 11f, 7f, 3f, 0f, 0f, 0f, 0f, 0f));

    fun sample(index: Int) = SpeedSample((index + 1) * 1_000_000_000L, speeds[index])
}
