package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoOpOperationsStreamDataSource : OperationsStreamDataSource {
    override fun observePoints(): Flow<List<OperationPoint>> = flowOf(emptyList())

    override fun observeStaff(): Flow<List<OperationStaff>> = flowOf(emptyList())

    override fun observeAssignments(): Flow<List<OperationAssignment>> = flowOf(emptyList())

    override fun observeInstructions(): Flow<List<OperationInstruction>> = flowOf(emptyList())

    override fun observeThreads(): Flow<List<OperationThread>> = flowOf(emptyList())

    override fun observeMessages(): Flow<List<OperationMessage>> = flowOf(emptyList())

    override fun start() = Unit

    override fun stop() = Unit
}
