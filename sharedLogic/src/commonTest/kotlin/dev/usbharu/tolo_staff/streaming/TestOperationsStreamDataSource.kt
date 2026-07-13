package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class TestOperationsStreamDataSource(
    points: List<OperationPoint> = emptyList(),
    staff: List<OperationStaff> = emptyList(),
    assignments: List<OperationAssignment> = emptyList(),
    instructions: List<OperationInstruction> = emptyList(),
    threads: List<OperationThread> = emptyList(),
    messages: List<OperationMessage> = emptyList(),
) : OperationsStreamDataSource {
    val pointsFlow = MutableStateFlow(points)
    val staffFlow = MutableStateFlow(staff)
    val assignmentsFlow = MutableStateFlow(assignments)
    val instructionsFlow = MutableStateFlow(instructions)
    val threadsFlow = MutableStateFlow(threads)
    val messagesFlow = MutableStateFlow(messages)
    var startCount = 0

    override fun observePoints(): Flow<List<OperationPoint>> = pointsFlow
    override fun observeStaff(): Flow<List<OperationStaff>> = staffFlow
    override fun observeAssignments(): Flow<List<OperationAssignment>> = assignmentsFlow
    override fun observeInstructions(): Flow<List<OperationInstruction>> = instructionsFlow
    override fun observeThreads(): Flow<List<OperationThread>> = threadsFlow
    override fun observeMessages(): Flow<List<OperationMessage>> = messagesFlow
    override fun start() { startCount += 1 }
    override fun stop() = Unit
}
