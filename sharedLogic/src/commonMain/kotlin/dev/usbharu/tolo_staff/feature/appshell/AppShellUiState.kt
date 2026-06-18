package dev.usbharu.tolo_staff.feature.appshell

data class AppShellUiState(
    val homeOverview: AppShellHomeOverview = AppShellHomeOverview(),
    val currentPlacementName: String = homeOverview.placementName,
    val selectedTab: AppTab = AppTab.HOME,
    val instructionsTab: InstructionsTabUiState = InstructionsTabUiState(),
    val reportsTab: ReportsTabUiState = ReportsTabUiState(),
    val contactsTab: ContactsTabUiState = ContactsTabUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
