package com.example.drivingcoach.ui.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Local display filters, not repository queries or a scoring policy. */
enum class ScorePeriod(val days: Long?) { WEEK(7), MONTH(30), ALL(null) }

@ConsistentCopyVisibility
data class ScoreTrendWindow internal constructor(
    val period: ScorePeriod,
    val points: List<ScoreObservation>,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val startAtMillis: Long?,
    val referenceAtMillis: Long?,
) {
    val measuredCount: Int = points.count { it.score != null }
    val missingCount: Int = points.size - measuredCount

    /** Compare the first and last records in this window, without skipping unmeasured endpoints. */
    val change: Int? = if (points.size >= 2 && points.first().completedAtMillis < points.last().completedAtMillis) {
        points.last().score?.let { last -> points.first().score?.let { last.value - it.value } }
    } else null

    /** Missing records and ambiguous equal timestamps break the line instead of inventing a value. */
    val segments: List<List<ScoreObservation>> = buildList {
        var segment = mutableListOf<ScoreObservation>()
        for (point in points) {
            if (point.score == null || segment.lastOrNull()?.completedAtMillis == point.completedAtMillis) {
                if (segment.isNotEmpty()) add(segment.toList())
                segment = mutableListOf()
            }
            if (point.score != null) segment.add(point)
        }
        if (segment.isNotEmpty()) add(segment.toList())
    }

    /** Real elapsed time determines horizontal spacing; a zero-width window has a centered point. */
    fun timeFraction(atMillis: Long): Float {
        val start = startAtMillis ?: return .5f
        val end = referenceAtMillis ?: return .5f
        if (end <= start) return .5f
        return ((atMillis.toDouble() - start.toDouble()) / (end.toDouble() - start.toDouble()))
            .coerceIn(0.0, 1.0).toFloat()
    }
}

/**
 * Calendar-day windows end at the latest supplied record, including a missing measurement.
 * No wall clock is consulted, so fixed Mock data remains stable in Preview and after app restarts.
 */
fun buildScoreTrendWindow(
    observations: List<ScoreObservation>,
    period: ScorePeriod,
    zoneId: ZoneId,
): ScoreTrendWindow {
    val ordered = observations.sortedBy { it.completedAtMillis }
    val reference = ordered.lastOrNull()?.completedAtMillis
        ?: return ScoreTrendWindow(period, emptyList(), null, null, null, null)
    val endDate = Instant.ofEpochMilli(reference).atZone(zoneId).toLocalDate()
    val startDate = period.days?.let { endDate.minusDays(it - 1) }
        ?: Instant.ofEpochMilli(ordered.first().completedAtMillis).atZone(zoneId).toLocalDate()
    val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    return ScoreTrendWindow(period, ordered.filter { it.completedAtMillis >= start },
        startDate, endDate, start, reference)
}
