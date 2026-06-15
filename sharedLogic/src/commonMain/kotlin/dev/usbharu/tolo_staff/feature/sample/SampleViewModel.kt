package dev.usbharu.tolo_staff.feature.sample

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class SampleViewModel(
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<SampleUiState, SampleEffect>(
    initialState = SampleUiState(),
    coroutineContext = coroutineContext
) {
    fun onPrimaryActionClicked() {
        updateState {
            it.copy(
                tapCount = it.tapCount + 1,
                lastAction = "primary"
            )
        }
        sendEffect(SampleEffect.ShowTapped)
    }
}
