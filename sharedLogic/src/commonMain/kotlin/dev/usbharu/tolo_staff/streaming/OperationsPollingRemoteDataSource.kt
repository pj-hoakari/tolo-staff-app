package dev.usbharu.tolo_staff.streaming

interface OperationsPollingRemoteDataSource {
    suspend fun listPoints(): List<OperationPoint>

    suspend fun listStaff(): List<OperationStaff>

    suspend fun listAssignments(): List<OperationAssignment>

    suspend fun listInstructions(): List<OperationInstruction>

    suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction>

    suspend fun listThreads(): List<OperationThread>

    suspend fun listMessages(): List<OperationMessage>

    suspend fun listThreadMessages(threadId: String): List<OperationMessage>
}
