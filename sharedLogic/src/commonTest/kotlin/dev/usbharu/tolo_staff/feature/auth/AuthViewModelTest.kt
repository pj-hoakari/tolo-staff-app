package dev.usbharu.tolo_staff.feature.auth

import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Test
    fun `view model reflects repository state changes`() {
        val repository = AuthRepository(AppSessionContext("tenant-a", "event-1", "fallback"))
        val viewModel = AuthViewModel(repository, UnconfinedTestDispatcher())

        viewModel.onLoginStarted()
        assertEquals(AuthPhase.AUTHORIZING, viewModel.uiState.value.phase)

        viewModel.onTokenExchangeStarted()
        assertEquals(AuthPhase.EXCHANGING, viewModel.uiState.value.phase)

        viewModel.onAuthenticated(validTokens())
        assertTrue(viewModel.uiState.value.isAuthenticated)
        viewModel.clear()
    }

    @Test
    fun `cancelled login returns a retryable signed out state`() {
        val repository = AuthRepository(AppSessionContext("tenant-a", "event-1", "fallback"))
        val viewModel = AuthViewModel(repository, UnconfinedTestDispatcher())

        viewModel.onLoginFailed(AuthFailure.CANCELLED)

        assertEquals(AuthPhase.SIGNED_OUT, viewModel.uiState.value.phase)
        assertEquals(AuthFailure.CANCELLED, viewModel.uiState.value.failure)
        viewModel.clear()
    }
}
