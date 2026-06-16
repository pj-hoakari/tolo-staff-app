package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.AppShellOperationsProjection
import dev.usbharu.tolo_staff.streaming.CurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @Test
    fun `initial state reflects streamed overview`() = runTest {
        val repository = FakeOperationsOverviewRepository(
            AppShellOperationsProjection(
                homeOverview = AppShellHomeOverview(
                    eventName = "Tolo Staff Demo 2026",
                    eventTime = "Firestore Streaming Demo",
                    placementName = "Gate A",
                    placementDetail = "North entrance",
                    currentInstruction = "Shift update: Move barricades"
                ),
                currentPlacementName = "Gate A"
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
        )

        assertEquals("Gate A", viewModel.uiState.value.currentPlacementName)
        assertEquals("Tolo Staff Demo 2026", viewModel.uiState.value.homeOverview.eventName)
        assertEquals("North entrance", viewModel.uiState.value.homeOverview.placementDetail)
        assertEquals("Shift update: Move barricades", viewModel.uiState.value.homeOverview.currentInstruction)
        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(false, viewModel.uiState.value.isLoading)
        viewModel.clear()
    }

    @Test
    fun `tab selection updates selected tab`() = runTest {
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
        )

        listOf(
            AppTab.INSTRUCTIONS,
            AppTab.REPORTS,
            AppTab.CONTACTS,
            AppTab.HOME
        ).forEach { tab ->
            viewModel.onTabSelected(tab)
            assertEquals(tab, viewModel.uiState.value.selectedTab)
        }

        viewModel.clear()
    }
}

private class FakeOperationsOverviewRepository(
    initialProjection: AppShellOperationsProjection = AppShellOperationsProjection(
        homeOverview = AppShellHomeOverview(),
        currentPlacementName = "未配属"
    )
) : OperationsOverviewRepository {
    private val projectionFlow = MutableStateFlow(initialProjection)

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> = projectionFlow
}

private class FixedCurrentStaffProviderForTest : CurrentStaffProvider {
    override val currentStaffId: String = "tanaka"
}
