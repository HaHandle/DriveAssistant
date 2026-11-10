package com.example.drivingcoach.data

import com.example.drivingcoach.data.mock.MockDriverOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Device capability selects the source, independently of transient vehicle UX restrictions. */
fun createDriverOverviewRepository(automotive: Boolean): DriverOverviewRepository =
    if (automotive) PendingVehicleOverviewRepository else MockDriverOverviewRepository()

/** No vehicle data source is connected yet. Never substitute sample scores in a car. */
private object PendingVehicleOverviewRepository : DriverOverviewRepository {
    override fun observeOverview(): Flow<DriverOverviewState> = flowOf(DriverOverviewState.Empty)
}
