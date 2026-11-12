package com.example.drivingcoach

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import com.example.drivingcoach.ui.LocalCoachMotion
import com.example.drivingcoach.ui.ScoreTrendPanel
import com.example.drivingcoach.ui.model.DrivingScore
import com.example.drivingcoach.ui.model.ScoreObservation
import com.example.drivingcoach.ui.model.ScorePeriod
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

class ScoreTrendPanelTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val normal get() = (MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready)
        .overview.score.trend

    private fun show(points: List<ScoreObservation>) {
        compose.setContent {
            var period by remember { mutableStateOf(ScorePeriod.WEEK) }
            DrivingCoachUITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ScoreTrendPanel(points, period, { period = it }, zoneId = ZoneId.of("UTC"))
                    }
                }
            }
        }
    }

    @Test fun periodSelectionChangesRecordsAndComparisonTogether() {
        show(normal)
        compose.onNodeWithText(context.getString(R.string.dashboard_record_counts, 4, 0)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_period_change, 6)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_period_month)).performClick().assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.dashboard_record_counts, 8, 0)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_period_change, 17)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_period_all)).performClick().assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.dashboard_record_counts, 9, 0)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_period_change, 20)).assertIsDisplayed()
    }

    @Test fun missingMeasurementsHaveANoticeAndNoZeroScoreChart() {
        show(normal.takeLast(2).map { it.copy(score = null) })
        compose.onNodeWithTag("score-trend-unmeasured").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("score-trend-chart").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.dashboard_record_counts, 0, 2))
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun oneMeasuredZeroStillHasAChartAndNoInventedComparison() {
        show(listOf(normal.last().copy(score = DrivingScore(0))))
        compose.onNodeWithTag("score-trend-chart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("score-trend-unmeasured").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.dashboard_no_comparison))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_record_counts, 1, 0))
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun anEmptyTrendDoesNotFabricateAChartOrDates() {
        show(emptyList())
        compose.onNodeWithTag("score-trend-empty").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("score-trend-chart").assertDoesNotExist()
        compose.onNodeWithTag("score-period-range").assertDoesNotExist()
    }

    @Test fun dataRefreshReplacesContentWithoutStartingAPeriodFade() {
        compose.mainClock.autoAdvance = false
        var points by mutableStateOf(emptyList<ScoreObservation>())
        compose.setContent {
            CompositionLocalProvider(LocalCoachMotion provides true) {
                DrivingCoachUITheme {
                    ScoreTrendPanel(points, ScorePeriod.WEEK, {}, zoneId = ZoneId.of("UTC"))
                }
            }
        }
        compose.onNodeWithTag("score-trend-empty").assertIsDisplayed()
        compose.runOnIdle { points = listOf(normal.last().copy(score = DrivingScore(0))) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("score-trend-chart").assertExists()
        compose.onNodeWithTag("score-trend-empty").assertDoesNotExist()
        compose.onAllNodesWithTag("score-trend-content-week").assertCountEquals(1)
    }

    @Test fun disablingMotionDuringAPeriodFadeLeavesOnlyTheTargetContent() {
        compose.mainClock.autoAdvance = false
        var period by mutableStateOf(ScorePeriod.WEEK)
        var motion by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalCoachMotion provides motion) {
                DrivingCoachUITheme {
                    ScoreTrendPanel(normal, period, { period = it }, zoneId = ZoneId.of("UTC"))
                }
            }
        }
        compose.runOnIdle { period = ScorePeriod.MONTH }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("score-trend-content-week").assertExists()
        compose.onNodeWithTag("score-trend-content-month").assertExists()
        compose.runOnIdle { motion = false }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("score-trend-content-week").assertDoesNotExist()
        compose.onAllNodesWithTag("score-trend-content-month").assertCountEquals(1)
    }
}
