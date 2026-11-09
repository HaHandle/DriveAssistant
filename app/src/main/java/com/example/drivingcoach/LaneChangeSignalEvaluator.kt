package com.example.drivingcoach

internal enum class IndicatorState(val label: String) {
    Off("꺼짐"), Left("왼쪽"), Right("오른쪽"), Hazard("비상등"), Unavailable("확인 불가")
}

internal enum class LaneChangeSide(val indicator: IndicatorState) {
    Left(IndicatorState.Left), Right(IndicatorState.Right)
}

internal data class TravelPoint(val elapsedMillis: Long, val distanceMeters: Double)

internal data class IndicatorObservation(val point: TravelPoint, val state: IndicatorState)

internal data class LaneChangeWindow(
    val side: LaneChangeSide,
    val start: TravelPoint,
    val finish: TravelPoint
)

internal enum class SignalOnset { InAdvance, Late, WrongDirection, Missing, Unobserved }
internal enum class SignalHold { Maintained, Interrupted, Unobserved }
internal enum class SignalClear { InTime, LeftOn, OtherDirection, Reactivated, Unobserved }

internal data class LaneChangeSignalReport(
    val onset: SignalOnset = SignalOnset.Unobserved,
    val hold: SignalHold = SignalHold.Unobserved,
    val clear: SignalClear = SignalClear.Unobserved,
    val leadMeters: Double? = null,
    val clearMeters: Double? = null
)

/** Grades an ordinary-road practice opportunity whose lane-change markers come from a separate source. */
internal object LaneChangeSignalEvaluator {
    const val MIN_LEAD_METERS = 30
    const val CLEAR_BEFORE_METERS = 10

    /**
     * Each observation is a sustained indicator state from its timestamp until the next observation.
     * The first and last observations mark the bounds of continuous coverage.
     */
    fun evaluate(
        window: LaneChangeWindow,
        observations: List<IndicatorObservation>
    ): LaneChangeSignalReport {
        if (!hasReliableCoverage(window, observations)) return LaneChangeSignalReport()

        val expected = window.side.indicator
        val startMillis = window.start.elapsedMillis
        val finishMillis = window.finish.elapsedMillis
        val throughStart = observations.takeWhile { it.point.elapsedMillis <= startMillis }
        val stateAtStart = throughStart.last().state
        val duringChange = observations.drop(throughStart.size)
            .takeWhile { it.point.elapsedMillis < finishMillis }

        val lastDifferent = throughStart.indexOfLast { it.state != expected }
        val lead = if (stateAtStart == expected && lastDifferent >= 0) {
            window.start.distanceMeters - throughStart[lastDifferent + 1].point.distanceMeters
        } else null
        val onset = when {
            stateAtStart == expected && lead == null -> SignalOnset.Unobserved
            lead != null -> if (lead >= MIN_LEAD_METERS) SignalOnset.InAdvance else SignalOnset.Late
            stateAtStart != IndicatorState.Off -> SignalOnset.WrongDirection
            duringChange.any { it.state == expected } -> SignalOnset.Late
            else -> SignalOnset.Missing
        }
        val hold = if (stateAtStart == expected && duringChange.all { it.state == expected }) {
            SignalHold.Maintained
        } else {
            SignalHold.Interrupted
        }

        val stateBeforeFinish = duringChange.lastOrNull()?.state ?: stateAtStart
        if (stateBeforeFinish != expected) return LaneChangeSignalReport(onset, hold, leadMeters = lead)

        val firstExit = observations.firstOrNull {
            it.point.elapsedMillis >= finishMillis && it.state != expected
        }
        val observedUntil = firstExit?.point ?: observations.last().point
        val distanceAfterFinish = observedUntil.distanceMeters - window.finish.distanceMeters
        val firstOff = firstExit?.takeIf { it.state == IndicatorState.Off }
        val signaledAgain = firstOff != null && observations.any {
            it.point.elapsedMillis > firstOff.point.elapsedMillis && it.state != IndicatorState.Off
        }
        val clear = when {
            distanceAfterFinish >= CLEAR_BEFORE_METERS -> SignalClear.LeftOn
            signaledAgain -> SignalClear.Reactivated
            firstOff != null -> SignalClear.InTime
            firstExit != null -> SignalClear.OtherDirection
            else -> SignalClear.Unobserved
        }
        val clearMeters = if (firstOff != null) distanceAfterFinish else null
        return LaneChangeSignalReport(onset, hold, clear, lead, clearMeters)
    }

    private fun hasReliableCoverage(
        window: LaneChangeWindow,
        observations: List<IndicatorObservation>
    ): Boolean {
        if (!window.start.isValid() || !window.finish.isValid() ||
            window.start.elapsedMillis >= window.finish.elapsedMillis ||
            window.start.distanceMeters >= window.finish.distanceMeters
        ) return false
        if (observations.isEmpty() || observations.any {
                !it.point.isValid() || it.state == IndicatorState.Unavailable || it.state == IndicatorState.Hazard
            }
        ) return false
        if (observations.zipWithNext().any { (previous, next) ->
                previous.point.elapsedMillis >= next.point.elapsedMillis ||
                    previous.point.distanceMeters > next.point.distanceMeters
            }
        ) return false

        return window.start.fits(observations) && window.finish.fits(observations)
    }

    private fun TravelPoint.isValid() =
        elapsedMillis >= 0L && distanceMeters.isFinite() && distanceMeters >= 0.0

    private fun TravelPoint.fits(observations: List<IndicatorObservation>): Boolean {
        val before = observations.lastOrNull { it.point.elapsedMillis <= elapsedMillis } ?: return false
        val after = observations.firstOrNull { it.point.elapsedMillis >= elapsedMillis } ?: return false
        return distanceMeters in before.point.distanceMeters..after.point.distanceMeters
    }
}
