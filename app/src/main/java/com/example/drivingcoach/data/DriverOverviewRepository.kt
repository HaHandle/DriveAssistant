package com.example.drivingcoach.data

import com.example.drivingcoach.ui.model.DriverOverview
import kotlinx.coroutines.flow.Flow

/** UI data boundary. Transport, authentication and backend DTOs are intentionally unspecified. */
interface DriverOverviewRepository {
    fun observeOverview(): Flow<DriverOverviewState>
}

sealed interface DriverOverviewState {
    data object Loading : DriverOverviewState
    data object Empty : DriverOverviewState
    data class Ready(val overview: DriverOverview) : DriverOverviewState
    data class Error(val reason: OverviewFailure) : DriverOverviewState
}

enum class OverviewFailure { UNAVAILABLE }
