package dev.usbharu.tolo_staff.feature.appshell

data class AppShellHomeOverview(
    val eventName: String = "",
    val eventTime: String = "",
    val placementName: String = "",
    val placementDetail: String = "",
    val currentInstruction: String = "",
    val currentInstructionTitle: String? = null,
    val currentInstructionTargetName: String? = null,
    val currentInstructionPriorityLabel: String? = null,
    val currentInstructionStatusLabel: String? = null,
    val currentInstructionLocationLabel: String? = null,
    val currentInstructionAttachmentSummary: String? = null,
    val currentInstructionUnreadCount: Int = 0,
    val mapState: AppShellMapState = AppShellMapState(),
    val currentInstructionId: String? = null,
    val unreadContactCount: Int = 0,
    val pendingReportLabel: String = "",
)
