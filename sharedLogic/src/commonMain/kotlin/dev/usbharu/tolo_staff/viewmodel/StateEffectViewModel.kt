package dev.usbharu.tolo_staff.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val effects = MutableSharedFlow<E>(extraBufferCapacity = 16)

    protected fun updateState(reducer: (S) -> S) {
        _uiState.value = reducer(_uiState.value)
    }

    protected fun sendEffect(effect: E) {
        effects.tryEmit(effect)
    }

    fun observeUiState(onChange: (S) -> Unit): Job =
        uiState.onEach(onChange).launchIn(viewModelScope)

    fun observeEffect(onChange: (E) -> Unit): Job =
        effects.onEach(onChange).launchIn(viewModelScope)

    open fun clear() {
        viewModelScope.cancel()
    }
}
