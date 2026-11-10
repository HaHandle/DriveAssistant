package com.example.drivingcoach.data.mock

import com.example.drivingcoach.data.DriverOverviewRepository
import com.example.drivingcoach.data.DriverOverviewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** One deterministic snapshot per collection. No network, sensors, timers or storage side effects. */
class MockDriverOverviewRepository(
    private val scenario: OverviewScenario = OverviewScenario.NORMAL,
) : DriverOverviewRepository {
    override fun observeOverview(): Flow<DriverOverviewState> = flowOf(MockDriverOverviewData.state(scenario))
}
