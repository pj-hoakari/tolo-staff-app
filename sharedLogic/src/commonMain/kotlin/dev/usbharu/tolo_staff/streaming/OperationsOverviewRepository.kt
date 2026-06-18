package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellMapState
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
    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        dataSource.start()
        return combine(
            dataSource.observePoints(),
            dataSource.observeAssignments(),
            dataSource.observeRelevantInstructions(currentStaffId),
        ) { points, assignments, instructions ->
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

        val placementName = currentPoint?.name ?: "未配属"
        val placementDetail = currentPoint?.description ?: "現在の配置情報はまだ共有されていません。"
        val instructionText = activeInstruction?.toDisplayText()
            ?: "現在有効な指示はありません。"

        return AppShellOperationsProjection(
            homeOverview = AppShellHomeOverview(
                eventName = "Tolo Staff Demo 2026",
                eventTime = "Firestore Streaming Demo",
                placementName = placementName,
                placementDetail = placementDetail,
                currentInstruction = instructionText,
                mapState = AppShellMapState()
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

    private val OperationAssignmentStatus.priority: Int
        get() = when (this) {
            OperationAssignmentStatus.ACTIVE -> 0
            OperationAssignmentStatus.EN_ROUTE -> 1
            OperationAssignmentStatus.PENDING -> 2
        }
}
