package dev.usbharu.tolo_staff.feature.appshell

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @Test
    fun `initial state shows Tokyo venue and home tab`() = runTest {
        val viewModel = AppShellViewModel(UnconfinedTestDispatcher(testScheduler))

        assertEquals("物販会場", viewModel.uiState.value.currentPlacementName)
        assertEquals("Tolo Staff Demo 2026", viewModel.uiState.value.homeOverview.eventName)
        assertEquals("物販会場", viewModel.uiState.value.homeOverview.placementName)
        assertEquals("西ホール 入口ゲート A", viewModel.uiState.value.homeOverview.placementDetail)
        assertEquals(
            "来場者導線を確保し、不明点はリーダーへ連絡してください。",
            viewModel.uiState.value.homeOverview.currentInstruction
        )
        assertEquals("東京ビッグサイト", viewModel.uiState.value.homeOverview.mapState.venueName)
        assertEquals(35.6300, viewModel.uiState.value.homeOverview.mapState.latitude)
        assertEquals(139.7946, viewModel.uiState.value.homeOverview.mapState.longitude)
        assertEquals(0.01, viewModel.uiState.value.homeOverview.mapState.latitudeDelta)
        assertEquals(0.01, viewModel.uiState.value.homeOverview.mapState.longitudeDelta)
        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        viewModel.clear()
    }

    @Test
    fun `tab selection updates selected tab`() = runTest {
        val viewModel = AppShellViewModel(UnconfinedTestDispatcher(testScheduler))

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
