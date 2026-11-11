package com.example.drivingcoach.data.mock

import com.example.drivingcoach.data.DriverOverviewRepository
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.OverviewFailure
import com.example.drivingcoach.ui.model.DriverProgress
import com.example.drivingcoach.ui.model.HabitStatus
import com.example.drivingcoach.ui.model.OverviewOrigin
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDriverOverviewRepositoryTest {
    @Test
    fun everyScenarioIsDeterministicAcrossRepeatedCollections() = runBlocking {
        OverviewScenario.entries.forEach { scenario ->
            val repository: DriverOverviewRepository = MockDriverOverviewRepository(scenario)
            assertEquals(repository.observeOverview().single(), repository.observeOverview().single())
        }
    }

    @Test
    fun normalSnapshotIsExplicitlyMockAndDoesNotAssignATier() = runBlocking {
        val state = MockDriverOverviewRepository().observeOverview().single() as DriverOverviewState.Ready
        assertEquals(OverviewOrigin.MOCK, state.overview.origin)
        assertEquals(DriverProgress.Unassigned, state.overview.progress)
        assertEquals(92, state.overview.score.current?.value)
        assertEquals(4, state.overview.score.change)
    }

    @Test
    fun loadingEmptyAndErrorAreNotSuccessfulZeroScoreSnapshots() = runBlocking {
        assertEquals(DriverOverviewState.Loading,
            MockDriverOverviewRepository(OverviewScenario.LOADING).observeOverview().single())
        assertEquals(DriverOverviewState.Empty,
            MockDriverOverviewRepository(OverviewScenario.EMPTY).observeOverview().single())
        assertEquals(DriverOverviewState.Error(OverviewFailure.UNAVAILABLE),
            MockDriverOverviewRepository(OverviewScenario.ERROR).observeOverview().single())
    }

    @Test
    fun partialDataPreservesUnmeasuredScoresAndUnobservedHabits() = runBlocking {
        val state = MockDriverOverviewRepository(OverviewScenario.PARTIAL).observeOverview().single()
            as DriverOverviewState.Ready
        assertNull(state.overview.score.current)
        assertNull(state.overview.score.change)
        assertTrue(state.overview.score.trend.any { it.score == null })
        val unobserved = state.overview.habits.filter { it.status == HabitStatus.UNOBSERVED }
        assertTrue(unobserved.any { it.observations == null })
        assertTrue(unobserved.any { it.observations?.observed == 0 })
    }

    @Test
    fun tierExamplesAreOptInAndRemainLabelledAsMock() = runBlocking {
        for (scenario in listOf(OverviewScenario.TIER_EXAMPLE, OverviewScenario.HIGHEST_TIER)) {
            val state = MockDriverOverviewRepository(scenario).observeOverview().single() as DriverOverviewState.Ready
            assertEquals(OverviewOrigin.MOCK, state.overview.origin)
            assertTrue(state.overview.progress is DriverProgress.Preview)
        }
    }
}
