package dev.usbharu.tolo_staff.feature.appshell

data class AppShellUiState(
    val currentPlacementName: String = "東京会場",
    val selectedTab: AppTab = AppTab.HOME
)
