package dev.usbharu.tolo_staff.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.CoroutineContext

abstract class StateEffectViewModel<S, E>(
    initialState: S,
    coroutineContext: CoroutineContext = Dispatchers.Default
) {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + coroutineContext)

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    protected val currentState: S
        get() = _uiState.value

    private val effects = Channel<E>(Channel.UNLIMITED)
    val effectFlow: Flow<E> = effects.receiveAsFlow()

    protected fun updateState(reducer: (S) -> S) {
        _uiState.value = reducer(_uiState.value)
    }

    protected fun sendEffect(effect: E) {
        check(effects.trySend(effect).isSuccess) { "View model effect queue is closed" }
    }

    fun observeUiState(onChange: (S) -> Unit): Job =
        uiState.onEach(onChange).launchIn(viewModelScope)

    fun observeEffect(onChange: (E) -> Unit): Job =
        effectFlow.onEach(onChange).launchIn(viewModelScope)

    open fun clear() {
        viewModelScope.cancel()
    }
}
