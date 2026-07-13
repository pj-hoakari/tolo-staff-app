package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.NoOpEventRepository
import dev.usbharu.tolo_staff.feature.appshell.OperationEvent
import dev.usbharu.tolo_staff.feature.appshell.PlacementPhase
import dev.usbharu.tolo_staff.feature.appshell.placementStatusFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppShellOperationsProjection(
    val homeOverview: AppShellHomeOverview,
    val currentPlacementName: String,
    val currentAssignmentId: String? = null,
    val currentAssignmentStatus: OperationAssignmentStatus? = null,
    val currentAssignmentVersion: Long = 0,
)

interface OperationsOverviewRepository {
    fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection>
}

class OperationsOverviewRepositoryImpl(
    private val dataSource: OperationsStreamDataSource,
    private val eventRepository: EventRepository = NoOpEventRepository(),
) : OperationsOverviewRepository {
    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        dataSource.start()
        return combine(
            dataSource.observePoints(),
            dataSource.observeAssignments(),
            dataSource.observeRelevantInstructions(currentStaffId),
            eventRepository.observeCurrentEvent(),
        ) { points, assignments, instructions, event ->
            buildProjection(currentStaffId, points, assignments, instructions, event)
        }
    }

    private fun buildProjection(
        currentStaffId: String,
        points: List<OperationPoint>,
        assignments: List<OperationAssignment>,
        instructions: List<OperationInstruction>,
        event: OperationEvent,
    ): AppShellOperationsProjection {
        val currentAssignment = assignments
            .filter { it.staffId == currentStaffId && it.status in CURRENT_STATUSES }
            .minByOrNull { if (it.status == OperationAssignmentStatus.ACTIVE) 0 else 1 }
        val placementName = points.firstOrNull { it.pointId == currentAssignment?.pointId }?.name.orEmpty()
        val currentInstruction = instructions
            .filter { it.status == OperationInstructionStatus.ISSUED }
            .maxWithOrNull(compareBy<OperationInstruction> { it.updatedAt }.thenBy { it.instructionId })
        val targetName = currentInstruction?.targetLabel(points, currentStaffId)
        val placementPhase = if (currentAssignment?.status == OperationAssignmentStatus.SCHEDULED) {
            PlacementPhase.SCHEDULED
        } else {
            PlacementPhase.ACTIVE
        }
        return AppShellOperationsProjection(
            homeOverview = AppShellHomeOverview(
                eventName = event.name,
                eventTime = formatEventTimeLabel(event.startTimeIso, event.endTimeIso),
                placementName = placementName,
                currentInstruction = currentInstruction?.body.orEmpty(),
                currentInstructionTitle = currentInstruction?.title,
                currentInstructionTargetName = targetName,
                currentInstructionStatusLabel = currentInstruction?.status?.toLabel(),
                currentInstructionId = currentInstruction?.instructionId,
                placementStatus = placementStatusFor(
                    assignId = currentAssignment?.assignId,
                    expectedVersion = currentAssignment?.version ?: 0,
                    placementName = placementName,
                    phase = placementPhase,
                ),
            ),
            currentPlacementName = placementName,
            currentAssignmentId = currentAssignment?.assignId,
            currentAssignmentStatus = currentAssignment?.status,
            currentAssignmentVersion = currentAssignment?.version ?: 0,
        )
    }

    private companion object {
        val CURRENT_STATUSES = setOf(OperationAssignmentStatus.ACTIVE, OperationAssignmentStatus.SCHEDULED)
    }
}

internal fun OperationInstruction.targetLabel(points: List<OperationPoint>, currentStaffId: String): String = when (targetType) {
    OperationInstructionTargetType.ALL_EVENT_STAFF -> "イベント全体"
    OperationInstructionTargetType.STAFF -> if (targetId == currentStaffId) "あなた" else targetId.orEmpty()
    OperationInstructionTargetType.POINT -> points.firstOrNull { it.pointId == targetId }?.name ?: targetId.orEmpty()
    OperationInstructionTargetType.UNKNOWN -> "対象不明"
}

internal fun OperationInstructionStatus.toLabel(): String = when (this) {
    OperationInstructionStatus.ISSUED -> "対応中"
    OperationInstructionStatus.COMPLETED -> "完了"
    OperationInstructionStatus.CANCELLED -> "キャンセル"
    OperationInstructionStatus.EXPIRED -> "期限切れ"
}

internal fun List<OperationInstruction>.relevantTo(
    currentStaffId: String,
    assignments: List<OperationAssignment>,
): List<OperationInstruction> {
    val pointIds = assignments
        .filter { it.staffId == currentStaffId && it.status in setOf(OperationAssignmentStatus.ACTIVE, OperationAssignmentStatus.SCHEDULED) }
        .mapTo(mutableSetOf()) { it.pointId }
    return filter {
        it.targetType == OperationInstructionTargetType.ALL_EVENT_STAFF ||
            it.targetType == OperationInstructionTargetType.STAFF && it.targetId == currentStaffId ||
            it.targetType == OperationInstructionTargetType.POINT && it.targetId in pointIds
    }
}
