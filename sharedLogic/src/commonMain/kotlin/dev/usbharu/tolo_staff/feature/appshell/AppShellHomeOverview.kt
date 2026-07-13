package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus

data class AppShellHomeOverview(
    val eventName: String = "",
    val eventTime: String = "",
    val placementName: String = "",
    val currentInstruction: String = "",
    val currentInstructionTitle: String? = null,
    val currentInstructionTargetName: String? = null,
    val currentInstructionStatusLabel: String? = null,
    val currentInstructionId: String? = null,
    val unreadContactCount: Int = 0,
    val pendingReportLabel: String = "",
    val placementStatus: PlacementStatusUiModel = PlacementStatusUiModel(),
)

enum class PlacementPhase { SCHEDULED, ACTIVE }

data class PlacementStatusUiModel(
    val assignId: String? = null,
    val expectedVersion: Long = 0,
    val placementName: String = "",
    val phase: PlacementPhase = PlacementPhase.ACTIVE,
    val headline: String = "現在の配置",
    val buttonLabel: String? = null,
    val showsActionButton: Boolean = false,
)

fun placementStatusFor(
    assignId: String?,
    expectedVersion: Long,
    placementName: String,
    phase: PlacementPhase,
): PlacementStatusUiModel = when (phase) {
    PlacementPhase.SCHEDULED -> PlacementStatusUiModel(
        assignId = assignId,
        expectedVersion = expectedVersion,
        placementName = placementName,
        phase = phase,
        headline = "配置が変更されました",
        buttonLabel = "到着",
        showsActionButton = true,
    )
    PlacementPhase.ACTIVE -> PlacementStatusUiModel(
        assignId = assignId,
        expectedVersion = expectedVersion,
        placementName = placementName,
        phase = phase,
    )
}

fun OperationAssignmentStatus.toPlacementPhase(): PlacementPhase = when (this) {
    OperationAssignmentStatus.SCHEDULED -> PlacementPhase.SCHEDULED
    OperationAssignmentStatus.ACTIVE,
    OperationAssignmentStatus.ENDED,
    OperationAssignmentStatus.CANCELLED,
    -> PlacementPhase.ACTIVE
}
