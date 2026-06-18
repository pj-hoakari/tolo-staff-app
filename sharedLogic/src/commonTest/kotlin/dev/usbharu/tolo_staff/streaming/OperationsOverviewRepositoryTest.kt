package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OperationsOverviewRepositoryTest {
    @Test
    fun `overview derives placement and instruction from assignment and point`() = runTest {
        val dataSource = FakeOperationsStreamDataSource(
            points = listOf(
                OperationPoint(
                    updatedAt = "2026-06-16T12:34:56.789Z",
                    reason = "point.updated",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance"
                )
            ),
            assignments = listOf(
                OperationAssignment(
                    updatedAt = "2026-06-16T12:34:56.789Z",
                    reason = "assignment.updated",
                    entityId = "assign-1",
                    assignId = "assign-1",
                    pointId = "gate-a",
                    staffId = "tanaka",
                    status = OperationAssignmentStatus.ACTIVE
                )
            ),
            instructions = listOf(
                OperationInstruction(
                    updatedAt = "2026-06-16T12:34:56.789Z",
                    reason = "instruction.updated",
                    entityId = "inst-1",
                    instructionId = "inst-1",
                    pointIds = listOf("gate-a"),
                    staffIds = emptyList(),
                    title = "Shift update",
                    description = "Move barricades",
                    status = OperationInstructionStatus.ACTIVE
                )
            )
        )
        val repository = OperationsOverviewRepositoryImpl(dataSource)

        val projection = repository.observeOverview("tanaka").first()

        assertEquals("Gate A", projection.currentPlacementName)
        assertEquals("North entrance", projection.homeOverview.placementDetail)
        assertEquals("Shift update: Move barricades", projection.homeOverview.currentInstruction)
        assertEquals("inst-1", projection.homeOverview.currentInstructionId)
    }
}

private class FakeOperationsStreamDataSource(
    points: List<OperationPoint> = emptyList(),
    staff: List<OperationStaff> = emptyList(),
    assignments: List<OperationAssignment> = emptyList(),
    instructions: List<OperationInstruction> = emptyList(),
    threads: List<OperationThread> = emptyList(),
    messages: List<OperationMessage> = emptyList(),
) : OperationsStreamDataSource {
    private val pointsFlow = MutableStateFlow(points)
    private val staffFlow = MutableStateFlow(staff)
    private val assignmentsFlow = MutableStateFlow(assignments)
    private val instructionsFlow = MutableStateFlow(instructions)
    private val threadsFlow = MutableStateFlow(threads)
    private val messagesFlow = MutableStateFlow(messages)

    override fun observePoints() = pointsFlow

    override fun observeStaff() = staffFlow

    override fun observeAssignments() = assignmentsFlow

    override fun observeInstructions() = instructionsFlow

    override fun observeThreads() = threadsFlow

    override fun observeMessages() = messagesFlow

    override fun start() = Unit

    override fun stop() = Unit
}
