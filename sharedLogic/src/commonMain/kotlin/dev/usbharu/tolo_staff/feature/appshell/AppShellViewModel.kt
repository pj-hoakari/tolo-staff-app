package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.CurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.FixedCurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

class AppShellViewModel(
    private val overviewRepository: OperationsOverviewRepository = OperationsOverviewRepositoryImpl(
        dataSource = NoOpOperationsStreamDataSource()
    ),
    private val currentStaffProvider: CurrentStaffProvider = FixedCurrentStaffProvider(),
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<AppShellUiState, Unit>(
    initialState = AppShellUiState(isLoading = true),
    coroutineContext = coroutineContext
) {
    private var overviewJob: Job? = null

    init {
        overviewJob = overviewRepository.observeOverview(currentStaffProvider.currentStaffId)
            .onEach { projection ->
                updateState {
                    it.copy(
                        homeOverview = projection.homeOverview,
                        currentPlacementName = projection.currentPlacementName,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            .catch { throwable ->
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "ホーム情報の購読に失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: AppTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    override fun clear() {
        overviewJob?.cancel()
        super.clear()
    }
}
