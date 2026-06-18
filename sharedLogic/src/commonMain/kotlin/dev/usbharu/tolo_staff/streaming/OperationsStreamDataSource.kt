package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow

interface OperationsStreamDataSource {
    fun observePoints(): Flow<List<OperationPoint>>

    fun observeStaff(): Flow<List<OperationStaff>>

    fun observeAssignments(): Flow<List<OperationAssignment>>

    fun observeInstructions(): Flow<List<OperationInstruction>>

    fun observeRelevantInstructions(currentStaffId: String): Flow<List<OperationInstruction>> = observeInstructions()

    fun observeThreads(): Flow<List<OperationThread>>

    fun observeMessages(): Flow<List<OperationMessage>>

    fun start()

    fun stop()
}
