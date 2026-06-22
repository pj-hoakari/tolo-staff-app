package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellMapState
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.NoOpEventRepository
import dev.usbharu.tolo_staff.feature.appshell.OperationEvent
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppShellOperationsProjection(
    val homeOverview: AppShellHomeOverview,
    val currentPlacementName: String,
    val currentAssignmentId: String? = null,
    val currentAssignmentStatus: OperationAssignmentStatus? = null,
)

interface OperationsOverviewRepository {
    fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection>
}

class OperationsOverviewRepositoryImpl(
    private val dataSource: OperationsStreamDataSource,
    private val eventRepository: EventRepository = NoOpEventRepository(),
) : OperationsOverviewRepository {
    private val logger = AppLogger.withTag("OperationsOverviewRepository")

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        logger.info { "observeOverview started: currentStaffId=$currentStaffId" }
        dataSource.start()
        return combine(
            dataSource.observePoints(),
            dataSource.observeAssignments(),
            dataSource.observeRelevantInstructions(currentStaffId),
            eventRepository.observeCurrentEvent(),
        ) { points, assignments, instructions, currentEvent ->
            logger.debug {
                "Building overview projection: currentStaffId=$currentStaffId, points=${points.size}, assignments=${assignments.size}, instructions=${instructions.size}"
            }
            buildProjection(
                currentStaffId = currentStaffId,
                points = points,
                assignments = assignments,
                instructions = instructions,
                currentEvent = currentEvent,
            )
        }
    }

    private fun buildProjection(
        currentStaffId: String,
        points: List<OperationPoint>,
        assignments: List<OperationAssignment>,
        instructions: List<OperationInstruction>,
        currentEvent: OperationEvent = OperationEvent(),
    ): AppShellOperationsProjection {
        val relevantInstructions = instructions.relevantTo(
            currentStaffId = currentStaffId,
            assignments = assignments,
        )
        val currentAssignment = assignments
            .filter { it.staffId == currentStaffId }
            .sortedBy { it.status.priority }
            .firstOrNull()
        val currentPoint = currentAssignment?.pointId?.let { pointId ->
            points.firstOrNull { it.pointId == pointId }
        }
        val activeInstruction = relevantInstructions
            .filter { it.status == OperationInstructionStatus.ACTIVE }
            .maxWithOrNull(
                compareBy<OperationInstruction> { it.updatedAt }
                    .thenBy { it.instructionId }
            )

        val placementName = currentPoint?.name.orEmpty()
        val placementDetail = currentPoint?.description.orEmpty()
        val instructionText = activeInstruction?.toDisplayText().orEmpty()
        val instructionTargetName = activeInstruction
            ?.pointIds
            ?.mapNotNull { pointId -> points.firstOrNull { it.pointId == pointId }?.name }
            ?.distinct()
            ?.joinToString(" / ")
            ?.ifBlank { null }
        val instructionLocationLabel = activeInstruction
            ?.pointIds
            ?.mapNotNull { pointId ->
                points.firstOrNull { it.pointId == pointId }
                    ?.description
                    ?.ifBlank { null }
            }
            ?.distinct()
            ?.joinToString(" / ")
            ?.ifBlank { null }

        return AppShellOperationsProjection(
            homeOverview = AppShellHomeOverview(
                eventName = currentEvent.name,
                eventTime = currentEvent.toEventTimeLabel(),
                placementName = placementName,
                placementDetail = placementDetail,
                currentInstruction = instructionText,
                currentInstructionTitle = activeInstruction?.title,
                currentInstructionTargetName = instructionTargetName,
                currentInstructionStatusLabel = activeInstruction?.status?.toStatusLabel(),
                currentInstructionLocationLabel = instructionLocationLabel,
                mapState = AppShellMapState(
                    venueName = currentEvent.venueName,
                    latitude = currentEvent.latitude,
                    longitude = currentEvent.longitude,
                    latitudeDelta = currentEvent.latitudeDelta,
                    longitudeDelta = currentEvent.longitudeDelta,
                ),
                currentInstructionId = activeInstruction?.instructionId,
            ),
            currentPlacementName = placementName,
            currentAssignmentId = currentAssignment?.assignId,
            currentAssignmentStatus = currentAssignment?.status,
        )
    }

    private fun OperationInstruction.toDisplayText(): String {
        if (description.isBlank()) {
            return title
        }
        return "$title: $description"
    }

    private fun OperationInstructionStatus.toStatusLabel(): String =
        when (this) {
            OperationInstructionStatus.ACTIVE -> "対応中"
        }

    private val OperationAssignmentStatus.priority: Int
        get() = when (this) {
            OperationAssignmentStatus.ACTIVE -> 0
            OperationAssignmentStatus.EN_ROUTE -> 1
            OperationAssignmentStatus.PENDING -> 2
        }
}

private fun OperationEvent.toEventTimeLabel(): String =
    formatEventTimeLabel(startTimeIso, endTimeIso)

internal fun List<OperationInstruction>.relevantTo(
    currentStaffId: String,
    assignments: List<OperationAssignment>,
): List<OperationInstruction> {
    val assignedPointIds = assignments
        .asSequence()
        .filter { it.staffId == currentStaffId }
        .map { it.pointId }
        .toSet()

    return filter { instruction ->
        currentStaffId in instruction.staffIds ||
            instruction.pointIds.any(assignedPointIds::contains)
    }
}
