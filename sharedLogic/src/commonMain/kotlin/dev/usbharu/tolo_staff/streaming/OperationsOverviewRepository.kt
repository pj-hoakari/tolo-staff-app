package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellMapState
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppShellOperationsProjection(
    val homeOverview: AppShellHomeOverview,
    val currentPlacementName: String,
)

interface OperationsOverviewRepository {
    fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection>
}

class OperationsOverviewRepositoryImpl(
    private val dataSource: OperationsStreamDataSource,
) : OperationsOverviewRepository {
    private val logger = AppLogger.withTag("OperationsOverviewRepository")

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        logger.info { "observeOverview started: currentStaffId=$currentStaffId" }
        dataSource.start()
        return combine(
            dataSource.observePoints(),
            dataSource.observeAssignments(),
            dataSource.observeRelevantInstructions(currentStaffId),
        ) { points, assignments, instructions ->
            logger.debug {
                "Building overview projection: currentStaffId=$currentStaffId, points=${points.size}, assignments=${assignments.size}, instructions=${instructions.size}"
            }
            buildProjection(
                currentStaffId = currentStaffId,
                points = points,
                assignments = assignments,
                instructions = instructions,
            )
        }
    }

    private fun buildProjection(
        currentStaffId: String,
        points: List<OperationPoint>,
        assignments: List<OperationAssignment>,
        instructions: List<OperationInstruction>,
    ): AppShellOperationsProjection {
        val currentAssignment = assignments
            .filter { it.staffId == currentStaffId }
            .sortedBy { it.status.priority }
            .firstOrNull()
        val currentPoint = currentAssignment?.pointId?.let { pointId ->
            points.firstOrNull { it.pointId == pointId }
        }
        val activeInstruction = instructions
            .filter { it.status == OperationInstructionStatus.ACTIVE }
            .filter { instruction ->
                currentPoint?.pointId in instruction.pointIds || currentStaffId in instruction.staffIds
            }
            .maxWithOrNull(
                compareBy<OperationInstruction> { it.updatedAt }
                    .thenBy { it.instructionId }
            )

        val placementName = currentPoint?.name.orEmpty()
        val placementDetail = currentPoint?.description.orEmpty()
        val instructionText = activeInstruction?.toDisplayText().orEmpty()

        return AppShellOperationsProjection(
            homeOverview = AppShellHomeOverview(
                placementName = placementName,
                placementDetail = placementDetail,
                currentInstruction = instructionText,
                currentInstructionTitle = activeInstruction?.title,
                currentInstructionTargetName = currentPoint?.name,
                currentInstructionStatusLabel = activeInstruction?.status?.toStatusLabel(),
                currentInstructionLocationLabel = currentPoint?.description,
                mapState = AppShellMapState(),
                currentInstructionId = activeInstruction?.instructionId,
            ),
            currentPlacementName = placementName
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
