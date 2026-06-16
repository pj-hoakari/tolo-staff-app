package dev.usbharu.tolo_staff.feature.appshell

data class AppShellHomeOverview(
    val eventName: String = "Tolo Staff Demo 2026",
    val eventTime: String = "受付開始 09:00 / 開場 10:00",
    val placementName: String = "物販会場",
    val placementDetail: String = "西ホール 入口ゲート A",
    val currentInstruction: String = "来場者導線を確保し、不明点はリーダーへ連絡してください。",
    val mapState: AppShellMapState = AppShellMapState()
)
