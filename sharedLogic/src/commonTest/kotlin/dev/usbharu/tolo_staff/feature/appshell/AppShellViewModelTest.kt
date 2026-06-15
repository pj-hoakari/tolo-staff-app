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

        assertEquals("東京会場", viewModel.uiState.value.currentPlacementName)
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
