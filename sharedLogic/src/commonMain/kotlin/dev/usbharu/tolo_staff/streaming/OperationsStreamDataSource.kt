package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface OperationsStreamDataSource {
    fun observePoints(): Flow<List<OperationPoint>>

    fun observeStaff(): Flow<List<OperationStaff>>

    fun observeAssignments(): Flow<List<OperationAssignment>>

    fun observeInstructions(): Flow<List<OperationInstruction>>

    fun observeRelevantInstructions(currentStaffId: String): Flow<List<OperationInstruction>> = observeInstructions()

    fun observeThreads(): Flow<List<OperationThread>>

    fun observeMessages(): Flow<List<OperationMessage>>

    fun observeMessages(currentStaffId: String): Flow<List<OperationMessage>> = observeMessages()

    fun observeThreadMessages(threadId: String, currentStaffId: String): Flow<List<OperationMessage>> =
        observeMessages(currentStaffId).map { messages ->
            messages.filter { it.threadId == threadId }
        }

    fun start()

    fun stop()
}
