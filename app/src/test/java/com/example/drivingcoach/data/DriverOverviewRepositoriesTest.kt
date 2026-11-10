package com.example.drivingcoach.data

import com.example.drivingcoach.ui.model.OverviewOrigin
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DriverOverviewRepositoriesTest {
    @Test fun automotiveSourceNeverSuppliesMockData() = runBlocking {
        assertEquals(DriverOverviewState.Empty,
            createDriverOverviewRepository(automotive = true).observeOverview().single())
    }

    @Test fun nonAutomotiveSourceExplicitlyUsesMockData() = runBlocking {
        val state = createDriverOverviewRepository(automotive = false).observeOverview().single()
            as DriverOverviewState.Ready
        assertEquals(OverviewOrigin.MOCK, state.overview.origin)
    }
}
