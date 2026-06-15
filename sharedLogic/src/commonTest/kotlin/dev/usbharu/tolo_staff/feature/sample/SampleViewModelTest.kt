package dev.usbharu.tolo_staff.feature.sample

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SampleViewModelTest {
    @Test
    fun `primary action updates state`() = runTest {
        val viewModel = SampleViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onPrimaryActionClicked()

        assertEquals(1, viewModel.uiState.value.tapCount)
        assertEquals("primary", viewModel.uiState.value.lastAction)
        viewModel.clear()
    }

    @Test
    fun `primary action sends effect`() = runTest {
        val viewModel = SampleViewModel(UnconfinedTestDispatcher(testScheduler))
        val effect = CompletableDeferred<SampleEffect>()
        val job = viewModel.observeEffect { effect.complete(it) }

        viewModel.onPrimaryActionClicked()

        assertEquals(SampleEffect.ShowTapped, withTimeout(1_000) { effect.await() })
        job.cancel()
        viewModel.clear()
    }

    @Test
    fun `observe ui state emits current and updated state`() = runTest {
        val viewModel = SampleViewModel(UnconfinedTestDispatcher(testScheduler))
        val states = mutableListOf<SampleUiState>()
        val updated = CompletableDeferred<Unit>()
        val job = viewModel.observeUiState {
            states.add(it)
            if (states.size == 2) {
                updated.complete(Unit)
            }
        }

        viewModel.onPrimaryActionClicked()

        withTimeout(1_000) { updated.await() }
        assertEquals(0, states[0].tapCount)
        assertEquals(1, states[1].tapCount)
        job.cancel()
        viewModel.clear()
    }
}
