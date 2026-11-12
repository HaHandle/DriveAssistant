package com.example.drivingcoach.ui

import androidx.lifecycle.ViewModelStore
import com.example.drivingcoach.data.DriverOverviewRepository
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.OverviewFailure
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriverOverviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val ready = MockDriverOverviewData.state(OverviewScenario.NORMAL)

    @Before fun setMain() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: DriverOverviewRepository) = DriverOverviewViewModel(repository).also {
        store.put("overview", it)
    }

    private fun TestScope.subscribe(viewModel: DriverOverviewViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    @Test fun repositoryStartsOnlyWhenUiSubscribes() = runTest {
        var collections = 0
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = flow { collections++; emit(ready) }
        })
        runCurrent()
        assertEquals(DriverOverviewState.Loading, vm.uiState.value)
        assertEquals(0, collections)
        subscribe(vm)
        runCurrent()
        assertEquals(1, collections)
        assertEquals(ready, vm.uiState.value)
    }

    @Test fun loadingAndPartialMeasurementsAreForwardedWithoutSubstitutingScores() = runTest {
        val source = MutableSharedFlow<DriverOverviewState>()
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = source
        })
        subscribe(vm)
        runCurrent()
        assertEquals(DriverOverviewState.Loading, vm.uiState.value)
        val partial = MockDriverOverviewData.state(OverviewScenario.PARTIAL)
        source.emit(partial)
        runCurrent()
        assertEquals(partial, vm.uiState.value)
        source.emit(DriverOverviewState.Empty)
        runCurrent()
        assertEquals(DriverOverviewState.Empty, vm.uiState.value)
    }

    @Test fun emptyRepositoryCompletionDisplaysEmptyInsteadOfLoadingForever() = runTest {
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = emptyFlow<DriverOverviewState>()
        })
        subscribe(vm)
        runCurrent()
        assertEquals(DriverOverviewState.Empty, vm.uiState.value)
    }

    @Test fun retryRecoversFromFailureWhileObtainingTheFlow() = runTest {
        var attempts = 0
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = if (++attempts == 1) throw IOException("Unavailable") else flowOf(ready)
        })
        subscribe(vm)
        runCurrent()
        assertEquals(DriverOverviewState.Error(OverviewFailure.UNAVAILABLE), vm.uiState.value)
        vm.retry()
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(ready, vm.uiState.value)
    }

    @Test fun streamFailureReplacesOldDataWithAnError() = runTest {
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = flow { emit(ready); throw IOException("Unavailable") }
        })
        subscribe(vm)
        runCurrent()
        assertEquals(DriverOverviewState.Error(OverviewFailure.UNAVAILABLE), vm.uiState.value)
    }

    @Test fun retryCancelsOldCollectionAndIgnoresItsLaterUpdates() = runTest {
        val sources = listOf(MutableSharedFlow<DriverOverviewState>(), MutableSharedFlow())
        var attempts = 0
        var cancellations = 0
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = flow {
                val source = sources[attempts++]
                try { source.collect { emit(it) } } finally { cancellations++ }
            }
        })
        subscribe(vm)
        runCurrent()
        sources[0].emit(ready)
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(1, cancellations)
        assertEquals(DriverOverviewState.Loading, vm.uiState.value)
        sources[1].emit(DriverOverviewState.Empty)
        sources[0].emit(ready)
        runCurrent()
        assertEquals(DriverOverviewState.Empty, vm.uiState.value)
        assertEquals(2, attempts)
    }

    @Test fun consumersShareOneCollectionAndStoppingClearsStaleState() = runTest {
        var collections = 0
        var cancellations = 0
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = flow {
                collections++
                try { emit(ready); awaitCancellation() } finally { cancellations++ }
            }
        })
        val first = subscribe(vm)
        val second = subscribe(vm)
        runCurrent()
        assertEquals(1, collections)
        first.cancel()
        runCurrent()
        assertEquals(0, cancellations)
        second.cancel()
        runCurrent()
        assertEquals(1, cancellations)
        assertEquals(DriverOverviewState.Loading, vm.uiState.value)
        subscribe(vm)
        runCurrent()
        assertEquals(2, collections)
        assertEquals(ready, vm.uiState.value)
    }

    @Test fun clearingViewModelCancelsTheSourceWithoutTurningCancellationIntoAnError() = runTest {
        var cancellations = 0
        val vm = viewModel(object : DriverOverviewRepository {
            override fun observeOverview() = flow {
                try { emit(ready); awaitCancellation() } finally { cancellations++ }
            }
        })
        subscribe(vm)
        runCurrent()
        store.clear()
        runCurrent()
        assertEquals(1, cancellations)
        assertEquals(ready, vm.uiState.value)
    }
}
