package dev.usbharu.tolo_staff.feature.appshell

data class AppShellHomeOverview(
    val eventName: String = "Tolo Staff Demo 2026",
    val eventTime: String = "Firestore Streaming Demo",
    val placementName: String = "配置情報を読み込み中",
    val placementDetail: String = "現在の配置情報はまだ共有されていません。",
    val currentInstruction: String = "現在有効な指示はありません。",
    val mapState: AppShellMapState = AppShellMapState(),
    val currentInstructionId: String? = null,
    val unreadContactCount: Int = 0,
    val pendingReportLabel: String = "報告はありません。",
)
