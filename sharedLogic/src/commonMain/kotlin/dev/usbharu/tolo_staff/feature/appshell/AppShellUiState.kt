package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.unknownCurrentStaffMember

data class AppShellUiState(
    val homeOverview: AppShellHomeOverview = AppShellHomeOverview(),
    val currentPlacementName: String = homeOverview.placementName,
    val currentStaff: CurrentStaffUiModel = unknownCurrentStaffMember().toUiModel(),
    val availableStaff: List<CurrentStaffUiModel> = emptyList(),
    val selectedTab: AppTab = AppTab.HOME,
    val instructionsTab: InstructionsTabUiState = InstructionsTabUiState(),
    val reportsTab: ReportsTabUiState = ReportsTabUiState(),
    val contactsTab: ContactsTabUiState = ContactsTabUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

val AppShellUiState.displayedSelectedTab: AppTab
    get() = if (
        contactsTab.selectedThreadBackDestination == ContactThreadBackDestination.REPORT_DETAIL &&
        contactsTab.selectedThread != null
    ) {
        AppTab.REPORTS
    } else {
        selectedTab
    }

private fun dev.usbharu.tolo_staff.streaming.CurrentStaffMember.toUiModel(): CurrentStaffUiModel =
    CurrentStaffUiModel(
        staffId = staffId,
        displayName = displayName,
        roleLabel = roleLabel,
    )
