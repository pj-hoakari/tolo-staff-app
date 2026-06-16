package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class AppShellViewModel(
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<AppShellUiState, Unit>(
    initialState = AppShellUiState(),
    coroutineContext = coroutineContext
) {
    fun onTabSelected(tab: AppTab) {
        updateState { it.copy(selectedTab = tab) }
    }
}
