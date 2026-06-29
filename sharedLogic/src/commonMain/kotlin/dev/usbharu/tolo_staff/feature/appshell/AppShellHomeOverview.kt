package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus

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
    val placementStatus: PlacementStatusUiModel = PlacementStatusUiModel(),
)

enum class PlacementPhase {
    PENDING_CHANGE,
    EN_ROUTE,
    ACTIVE,
}

data class PlacementStatusUiModel(
    val assignId: String? = null,
    val placementName: String = "",
    val phase: PlacementPhase = PlacementPhase.ACTIVE,
    val headline: String = "現在の配置",
    val buttonLabel: String? = null,
    val showsActionButton: Boolean = false,
)

fun placementStatusFor(
    assignId: String?,
    placementName: String,
    phase: PlacementPhase,
): PlacementStatusUiModel =
    when (phase) {
        PlacementPhase.PENDING_CHANGE -> PlacementStatusUiModel(
            assignId = assignId,
            placementName = placementName,
            phase = phase,
            headline = "配置が変更されました",
            buttonLabel = "確認しました",
            showsActionButton = true,
        )

        PlacementPhase.EN_ROUTE -> PlacementStatusUiModel(
            assignId = assignId,
            placementName = placementName,
            phase = phase,
            headline = "「$placementName」へ移動中",
            buttonLabel = "到着",
            showsActionButton = true,
        )

        PlacementPhase.ACTIVE -> PlacementStatusUiModel(
            assignId = assignId,
            placementName = placementName,
            phase = phase,
            headline = "現在の配置",
            buttonLabel = null,
            showsActionButton = false,
        )
    }

fun OperationAssignmentStatus.toPlacementPhase(): PlacementPhase =
    when (this) {
        OperationAssignmentStatus.PENDING -> PlacementPhase.PENDING_CHANGE
        OperationAssignmentStatus.EN_ROUTE -> PlacementPhase.EN_ROUTE
        OperationAssignmentStatus.ACTIVE -> PlacementPhase.ACTIVE
    }

fun PlacementPhase.toOperationAssignmentStatus(): OperationAssignmentStatus =
    when (this) {
        PlacementPhase.PENDING_CHANGE -> OperationAssignmentStatus.PENDING
        PlacementPhase.EN_ROUTE -> OperationAssignmentStatus.EN_ROUTE
        PlacementPhase.ACTIVE -> OperationAssignmentStatus.ACTIVE
    }
