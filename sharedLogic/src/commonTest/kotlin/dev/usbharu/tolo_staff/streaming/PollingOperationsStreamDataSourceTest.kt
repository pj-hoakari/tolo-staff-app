package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PollingOperationsStreamDataSourceTest {
    @Test
    fun `message polling normalizes ordering by updatedAt then messageId`() = runTest {
        val dataSource = PollingOperationsStreamDataSource(
            remoteDataSource = FakeOperationsPollingRemoteDataSource(
                messages = listOf(
                    operationMessage(messageId = "m-2", updatedAt = "2026-06-17T10:00:00Z"),
                    operationMessage(messageId = "m-1", updatedAt = "2026-06-17T10:00:00Z"),
                    operationMessage(messageId = "m-3", updatedAt = "2026-06-17T11:00:00Z"),
                )
            ),
            config = OperationsPollingConfig(
                host = "localhost",
                port = 8080,
                intervalMillis = 60_000,
            )
        )

        val messages = dataSource.observeMessages().first()

        assertEquals(listOf("m-1", "m-2", "m-3"), messages.map { it.messageId })
    }

    @Test
    fun `staff polling flow performs an independent fetch per subscriber`() = runTest {
        val remote = CountingOperationsPollingRemoteDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "2026-06-17T10:00:00Z",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "Tanaka",
                    roles = listOf("leader"),
                )
            )
        )
        val dataSource = PollingOperationsStreamDataSource(
            remoteDataSource = remote,
            config = OperationsPollingConfig(
                host = "localhost",
                port = 8080,
                intervalMillis = 60_000,
            )
        )

        val first = launch { dataSource.observeStaff().first() }
        val second = launch { dataSource.observeStaff().first() }

        first.join()
        second.join()

        assertEquals(2, remote.staffRequests)
    }
}

private class FakeOperationsPollingRemoteDataSource(
    private val points: List<OperationPoint> = emptyList(),
    private val staff: List<OperationStaff> = emptyList(),
    private val assignments: List<OperationAssignment> = emptyList(),
    private val instructions: List<OperationInstruction> = emptyList(),
    private val threads: List<OperationThread> = emptyList(),
    private val messages: List<OperationMessage> = emptyList(),
) : OperationsPollingRemoteDataSource {
    override suspend fun listPoints(): List<OperationPoint> = points

    override suspend fun listStaff(): List<OperationStaff> = staff

    override suspend fun listAssignments(): List<OperationAssignment> = assignments

    override suspend fun listInstructions(): List<OperationInstruction> = instructions

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> = instructions

    override suspend fun listThreads(): List<OperationThread> = threads

    override suspend fun listMessages(): List<OperationMessage> = messages

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> =
        messages.filter { it.threadId == threadId }
}

private class CountingOperationsPollingRemoteDataSource(
    private val staff: List<OperationStaff>,
) : OperationsPollingRemoteDataSource {
    var staffRequests: Int = 0
        private set

    override suspend fun listPoints(): List<OperationPoint> = emptyList()

    override suspend fun listStaff(): List<OperationStaff> {
        staffRequests += 1
        return staff
    }

    override suspend fun listAssignments(): List<OperationAssignment> = emptyList()

    override suspend fun listInstructions(): List<OperationInstruction> = emptyList()

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> = emptyList()

    override suspend fun listThreads(): List<OperationThread> = emptyList()

    override suspend fun listMessages(): List<OperationMessage> = emptyList()

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> = emptyList()
}

private fun operationMessage(
    messageId: String,
    updatedAt: String,
) = OperationMessage(
    updatedAt = updatedAt,
    reason = "test",
    entityId = messageId,
    messageId = messageId,
    threadId = "thread-1",
    staffId = "tanaka",
    messageType = OperationMessageType.SIMPLE,
    text = messageId,
)
