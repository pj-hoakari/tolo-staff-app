package dev.usbharu.tolo_staff.feature.auth

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

class AuthViewModel(
    private val repository: AuthRepository,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : StateEffectViewModel<AuthUiState, Unit>(repository.state.value, coroutineContext) {
    init {
        repository.state.onEach { next -> updateState { next } }.launchIn(viewModelScope)
    }

    fun onLoginStarted() = repository.startAuthorization()

    fun onTokenExchangeStarted() = repository.startTokenExchange()

    fun onAuthenticated(tokens: AuthTokens) = repository.authenticate(tokens)

    fun onLoginFailed(failure: AuthFailure) = repository.fail(failure)

    fun onLogout() = repository.logout()
}
