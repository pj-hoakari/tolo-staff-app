package dev.usbharu.tolo_staff.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class StateEffectViewModelTest {
    @Test
    fun `effect sent before subscription remains pending`() = runTest {
        val viewModel = TestViewModel(StandardTestDispatcher(testScheduler))

        viewModel.emit("pending")

        assertEquals("pending", viewModel.effectFlow.first())
        viewModel.clear()
    }

    private class TestViewModel(
        dispatcher: CoroutineContext,
    ) : StateEffectViewModel<Unit, String>(Unit, dispatcher) {
        fun emit(effect: String) = sendEffect(effect)
    }
}
