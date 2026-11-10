package com.example.drivingcoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.drivingcoach.data.DriverOverviewRepository
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.OverviewFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DriverOverviewViewModel(private val repository: DriverOverviewRepository) : ViewModel() {
    private val attempts = MutableStateFlow(0L)

    // One upstream collection shared by UI consumers. Lifecycle stop cancels it and clears stale UI.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DriverOverviewState> = attempts.flatMapLatest {
        flow {
            emit(DriverOverviewState.Loading)
            emitAll(repository.observeOverview().onEmpty { emit(DriverOverviewState.Empty) })
        }.catch { failure ->
            if (failure is CancellationException) throw failure
            emit(DriverOverviewState.Error(OverviewFailure.UNAVAILABLE))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        initialValue = DriverOverviewState.Loading,
    )

    fun retry() {
        attempts.update { it + 1 }
    }

    class Factory(private val repository: DriverOverviewRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == DriverOverviewViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return DriverOverviewViewModel(repository) as T
        }
    }
}
