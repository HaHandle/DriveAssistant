package com.example.drivingcoach.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.drivingcoach.R
import com.example.drivingcoach.ui.model.ScoreObservation
import com.example.drivingcoach.ui.model.ScorePeriod
import com.example.drivingcoach.ui.model.ScoreTrendWindow
import com.example.drivingcoach.ui.model.buildScoreTrendWindow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ScoreTrendPanel(
    observations: List<ScoreObservation>,
    period: ScorePeriod,
    onPeriod: (ScorePeriod) -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val window = remember(observations, period, zoneId) { buildScoreTrendWindow(observations, period, zoneId) }
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    CoachPanel(modifier.fillMaxWidth().testTag("score-trend-panel")) {
        Text(stringResource(R.string.dashboard_trend_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.dashboard_period_basis), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            LiquidSelection(
                labels = listOf(R.string.dashboard_period_week, R.string.dashboard_period_month,
                    R.string.dashboard_period_all).map { stringResource(it) },
                selected = period.ordinal,
                onSelect = { onPeriod(ScorePeriod.entries[it]) },
                vertical = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f,
                modifier = Modifier.fillMaxWidth().testTag("score-period-selector"),
            )
        }
        if (window.startDate != null && window.endDate != null) {
            Text(stringResource(R.string.dashboard_date_range,
                window.startDate.format(dates), window.endDate.format(dates)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("score-period-range"))
        }
        val change = window.change
        Text(if (change == null) stringResource(R.string.dashboard_no_comparison)
            else stringResource(R.string.dashboard_period_change, change),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("score-period-change"))
        Text(stringResource(R.string.dashboard_record_counts, window.measuredCount, window.missingCount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("score-period-counts"))

        val motion = LocalCoachMotion.current
        AnimatedContent(
            targetState = window,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                if (motion) fadeIn(tween(180)) togetherWith fadeOut(tween(90))
                else EnterTransition.None togetherWith ExitTransition.None
            },
            label = "scorePeriodChart",
        ) { selected ->
            when {
                selected.points.isEmpty() -> TrendNotice(R.string.dashboard_no_history, "score-trend-empty")
                selected.measuredCount == 0 -> TrendNotice(R.string.dashboard_no_measurements, "score-trend-unmeasured")
                else -> ScoreTrendChart(selected, zoneId)
            }
        }
        if (window.missingCount > 0) {
            Text(stringResource(R.string.dashboard_missing_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendNotice(message: Int, tag: String) {
    Text(stringResource(message), style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(tag))
}

@Composable
private fun ScoreTrendChart(window: ScoreTrendWindow, zoneId: ZoneId) {
    val colors = MaterialTheme.colorScheme
    val axisLabelHalfHeight = with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toPx() / 2f }
    val locale = LocalConfiguration.current.locales[0]
    val fullDates = remember(locale, zoneId) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale).withZone(zoneId)
    }
    val axisPattern = stringResource(R.string.dashboard_axis_date_pattern)
    val axisDates = remember(locale, axisPattern) { DateTimeFormatter.ofPattern(axisPattern, locale) }
    val recordDescriptions = window.points.map { point ->
        val date = fullDates.format(Instant.ofEpochMilli(point.completedAtMillis))
        val score = point.score
        if (score == null) stringResource(R.string.dashboard_missing_record, date)
        else stringResource(R.string.dashboard_measured_record, date, score.value)
    }.joinToString("; ")
    val description = stringResource(R.string.dashboard_chart_description, recordDescriptions)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dashboard_score_scale), style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier.height(220.dp).width(IntrinsicSize.Max)
                    .clearAndSetSemantics {},
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(100, 50, 0).forEach { value ->
                    Text(value.toString(), style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("score-trend-chart")
                    .clearAndSetSemantics { contentDescription = description }) {
                    val horizontalInset = 8.dp.toPx().coerceAtMost(size.width / 2f)
                    val verticalInset = maxOf(8.dp.toPx(), axisLabelHalfHeight).coerceAtMost(size.height / 2f)
                    val width = (size.width - horizontalInset * 2f).coerceAtLeast(0f)
                    val height = (size.height - verticalInset * 2f).coerceAtLeast(0f)
                    fun position(point: ScoreObservation): Offset = Offset(
                        horizontalInset + window.timeFraction(point.completedAtMillis) * width,
                        verticalInset + (1f - requireNotNull(point.score).value / 100f) * height,
                    )
                    for (level in listOf(0f, .5f, 1f)) {
                        val y = verticalInset + height * level
                        drawLine(colors.outlineVariant, Offset(horizontalInset, y),
                            Offset(horizontalInset + width, y), 1.dp.toPx())
                    }
                    window.segments.forEach { segment ->
                        val line = Path().apply {
                            segment.forEachIndexed { index, point ->
                                val at = position(point)
                                if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
                            }
                        }
                        drawPath(line, colors.secondary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        segment.forEach { point ->
                            drawCircle(colors.surface, 6.dp.toPx(), position(point))
                            drawCircle(colors.secondary, 4.dp.toPx(), position(point))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(window.startDate?.format(axisDates).orEmpty(), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Text(window.endDate?.format(axisDates).orEmpty(), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End)
                }
            }
        }
    }
}
