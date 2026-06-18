package dev.usbharu.tolo_staff.feature.appshell

data class AppShellUiState(
    val homeOverview: AppShellHomeOverview = AppShellHomeOverview(),
    val currentPlacementName: String = homeOverview.placementName,
    val currentStaff: CurrentStaffUiModel = CurrentStaffUiModel(
        staffId = "tanaka",
        displayName = "田中",
        roleLabel = "Aゲート担当",
    ),
    val availableStaff: List<CurrentStaffUiModel> = emptyList(),
    val selectedTab: AppTab = AppTab.HOME,
    val instructionsTab: InstructionsTabUiState = InstructionsTabUiState(),
    val reportsTab: ReportsTabUiState = ReportsTabUiState(),
    val contactsTab: ContactsTabUiState = ContactsTabUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
