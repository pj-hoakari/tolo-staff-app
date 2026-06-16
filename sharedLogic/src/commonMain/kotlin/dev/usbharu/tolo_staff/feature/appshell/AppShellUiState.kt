package dev.usbharu.tolo_staff.feature.appshell

data class AppShellUiState(
    val homeOverview: AppShellHomeOverview = AppShellHomeOverview(),
    val currentPlacementName: String = homeOverview.placementName,
    val selectedTab: AppTab = AppTab.HOME
)
