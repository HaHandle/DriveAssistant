package com.example.drivingcoach

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.ui.model.DrivingHabitAnalysis
import com.example.drivingcoach.ui.model.HabitKind
import com.example.drivingcoach.ui.model.HabitStatus
import com.example.drivingcoach.ui.model.ObservationCount
import com.example.drivingcoach.vehicle.VehicleUxState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HabitAnalysisScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val normal get() = MockDriverOverviewData.state(OverviewScenario.NORMAL) as DriverOverviewState.Ready
    private fun text(id: Int) = context.getString(id)
    private fun counts(met: Int, observed: Int) = context.getString(R.string.habit_observations, met, observed)
    private fun withHabits(vararg habits: DrivingHabitAnalysis) =
        normal.copy(overview = normal.overview.copy(habits = habits.toList()))

    @Before
    fun resetPreferencesWithoutAnimation() {
        context.getSharedPreferences("coach_presentation", Context.MODE_PRIVATE).edit()
            .clear().putBoolean("motion", false).commit()
    }

    private fun open(kind: HabitKind) {
        compose.onNodeWithTag("habit-row-${kind.name}").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("habit-detail").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("habit-detail").assertIsDisplayed()
    }

    private fun scrollPosition(): Float = compose.onNodeWithTag("coach-home")
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test
    fun eachHabitOpensItsOwnDetailAndReturnsToSummary() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, normal) }
        val titles = mapOf(HabitKind.STOP_CONTROL to R.string.habit_stop_control,
            HabitKind.SIGNAL_TIMING to R.string.habit_signal_timing,
            HabitKind.SIGNAL_HOLD to R.string.habit_signal_hold,
            HabitKind.SIGNAL_CANCEL to R.string.habit_signal_cancel)
        for ((kind, title) in titles) {
            open(kind)
            compose.onNode(hasText(text(title)) and hasAnyAncestor(hasTestTag("habit-detail")))
                .assertIsDisplayed()
            compose.onNodeWithTag("habit-back").performClick()
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
            compose.onNodeWithTag("coach-home").assertIsDisplayed()
        }
    }

    @Test
    fun returningFromDetailKeepsHomeScrollAndSelectedScorePeriod() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, normal) }
        compose.onNodeWithText(text(R.string.dashboard_period_month)).performScrollTo().performClick()
        val row = compose.onNodeWithTag("habit-row-STOP_CONTROL")
        row.performScrollTo()
        val before = scrollPosition()
        row.performClick()
        compose.onNodeWithTag("habit-back").performClick()
        assertEquals(before, scrollPosition(), .5f)
        compose.onNodeWithText(text(R.string.dashboard_period_month)).performScrollTo().assertIsSelected()
    }

    @Test
    fun systemBackReturnsToTheCoachInsteadOfLeavingTheScreen() {
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            DrivingCoachApp(VehicleUxState.PREVIEW, true, normal)
        }
        open(HabitKind.STOP_CONTROL)
        compose.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        compose.onNodeWithTag("coach-home").assertIsDisplayed()
    }

    @Test
    fun savedSelectionRestoresAgainstCurrentReadyData() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, normal) }
        open(HabitKind.SIGNAL_HOLD)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("habit-detail").assertIsDisplayed()
        compose.onNode(hasText(text(R.string.habit_signal_hold)) and hasAnyAncestor(hasTestTag("habit-detail")))
            .assertIsDisplayed()
    }

    @Test
    fun sourceChangesUpdateAnOpenDetailWithoutKeepingPreviousCounts() {
        val state = mutableStateOf<DriverOverviewState>(withHabits(
            DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(2, 3))))
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, state.value) }
        open(HabitKind.STOP_CONTROL)
        compose.runOnIdle { state.value = withHabits(
            DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(0, 4))) }
        compose.onNodeWithText(counts(0, 4)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(counts(2, 3)).assertDoesNotExist()
    }

    @Test
    fun nonReadyStatesRemoveDetailsAndRecoveryDoesNotReopenThem() {
        val state = mutableStateOf<DriverOverviewState>(normal)
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, state.value) }
        for (scenario in listOf(OverviewScenario.LOADING, OverviewScenario.EMPTY, OverviewScenario.ERROR)) {
            open(HabitKind.STOP_CONTROL)
            compose.runOnIdle { state.value = MockDriverOverviewData.state(scenario) }
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
            compose.runOnIdle { state.value = normal }
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        }
    }

    @Test
    fun removingTheSelectedKindReturnsToTheCurrentSummary() {
        val state = mutableStateOf<DriverOverviewState>(normal)
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, state.value) }
        open(HabitKind.STOP_CONTROL)
        compose.runOnIdle { state.value = normal.copy(overview = normal.overview.copy(
            habits = normal.overview.habits.filter { it.kind != HabitKind.STOP_CONTROL })) }
        compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        compose.onNodeWithTag("habit-row-STOP_CONTROL").assertDoesNotExist()
        compose.onNodeWithTag("habit-row-SIGNAL_TIMING").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun vehiclePolicyRemovesDetailsDespiteAnInjectedMockOverview() {
        val vehicle = mutableStateOf(VehicleUxState.PREVIEW)
        compose.setContent { DrivingCoachApp(vehicle.value, true, normal) }
        for (next in listOf(VehicleUxState.AVAILABLE, VehicleUxState.UNAVAILABLE, VehicleUxState.RESTRICTED)) {
            open(HabitKind.STOP_CONTROL)
            compose.runOnIdle { vehicle.value = next }
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
            compose.onNodeWithTag("sample-data-label").assertDoesNotExist()
            compose.runOnIdle { vehicle.value = VehicleUxState.PREVIEW }
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        }
    }

    @Test
    fun missingEvidenceAndZeroObservationsHaveDifferentExplanations() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true,
            MockDriverOverviewData.state(OverviewScenario.PARTIAL)) }
        compose.onNodeWithText(text(R.string.habit_evidence_missing)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.habit_evidence_unobserved)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(counts(0, 0)).assertDoesNotExist()
        open(HabitKind.SIGNAL_HOLD)
        compose.onNodeWithText(text(R.string.habit_reason_missing)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("habit-back").performScrollTo().performClick()
        open(HabitKind.SIGNAL_CANCEL)
        compose.onNodeWithText(text(R.string.habit_reason_unobserved)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(counts(0, 0)).assertDoesNotExist()
    }

    @Test
    fun anObservedUnmetGoalDisplaysItsCountsAndReviewAssessment() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, withHabits(
            DrivingHabitAnalysis(HabitKind.STOP_CONTROL, HabitStatus.REVIEW, ObservationCount(0, 3)))) }
        open(HabitKind.STOP_CONTROL)
        compose.onNodeWithText(text(R.string.habit_review)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(counts(0, 3)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.habit_deferred)).assertDoesNotExist()
    }

    @Test
    fun unsupportedSuccessAndUnevaluatedCountsRemainDeferred() {
        val state = mutableStateOf<DriverOverviewState>(normal)
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, state.value) }
        val cases = listOf(HabitStatus.MET to ObservationCount(0, 3),
            HabitStatus.UNOBSERVED to ObservationCount(3, 3))
        for ((status, observations) in cases) {
            compose.runOnIdle { state.value = withHabits(
                DrivingHabitAnalysis(HabitKind.STOP_CONTROL, status, observations)) }
            open(HabitKind.STOP_CONTROL)
            compose.onNodeWithText(text(R.string.habit_deferred)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(counts(observations.met, observations.observed))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.habit_met)).assertDoesNotExist()
            compose.onNodeWithTag("habit-back").performScrollTo().performClick()
        }
    }

    @Test
    fun switchingTabsClearsTheDetailSelection() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, true, normal) }
        open(HabitKind.STOP_CONTROL)
        compose.onNodeWithText(text(R.string.tab_history)).performClick()
        compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        compose.onNodeWithText(text(R.string.tab_coach)).performClick()
        compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        compose.onNodeWithTag("coach-home").assertIsDisplayed()
    }

    @Test
    fun backgroundingDuringAnExitFadeRemovesTheOutgoingDetailImmediately() {
        context.getSharedPreferences("coach_presentation", Context.MODE_PRIVATE).edit()
            .putBoolean("motion", true).commit()
        val foreground = mutableStateOf(true)
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, foreground.value, normal) }
        open(HabitKind.STOP_CONTROL)
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("habit-back").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { foreground.value = false }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("habit-detail").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
