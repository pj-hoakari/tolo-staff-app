package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationAssignment
import dev.usbharu.tolo_staff.streaming.OperationInstruction
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcContactChatServiceTest {
    @Test
    fun `observe rooms uses display title and latest message order`() = runTest {
        val service = GrpcContactChatService(
            dataSource = FakeOperationsStreamDataSource(
                staff = listOf(
                    OperationStaff(updatedAt = "", reason = "test", entityId = "tanaka", staffId = "tanaka", name = "田中"),
                    OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤"),
                ),
                threads = listOf(
                    OperationThread(
                        updatedAt = "",
                        reason = "test",
                        entityId = "thread-old",
                        threadId = "thread-old",
                        members = listOf("tanaka", "sato"),
                    ),
                    OperationThread(
                        updatedAt = "",
                        reason = "test",
                        entityId = "thread-new",
                        threadId = "thread-new",
                        members = listOf("tanaka", "sato"),
                        displayTitle = "Aゲート連絡",
                    ),
                    OperationThread(
                        updatedAt = "",
                        reason = "test",
                        entityId = "thread-none",
                        threadId = "thread-none",
                        members = listOf("tanaka", "sato"),
                    ),
                ),
                messages = listOf(
                    OperationMessage(
                        updatedAt = "2026-06-19T09:00:00Z",
                        reason = "test",
                        entityId = "message-1",
                        messageId = "message-1",
                        threadId = "thread-old",
                        staffId = "sato",
                        messageType = OperationMessageType.SIMPLE,
                        text = "old",
                    ),
                    OperationMessage(
                        updatedAt = "2026-06-19T10:00:00Z",
                        reason = "test",
                        entityId = "message-2",
                        messageId = "message-2",
                        threadId = "thread-new",
                        staffId = "sato",
                        messageType = OperationMessageType.SIMPLE,
                        text = "new",
                    ),
                ),
            ),
            grpcClient = GrpcCommunicationClient(host = "127.0.0.1", port = 1),
        )

        val rooms = service.observeRooms(currentStaffId = "tanaka").first()

        assertEquals(listOf("thread-new", "thread-old", "thread-none"), rooms.map { it.id })
        assertEquals(listOf("Aゲート連絡", "佐藤", "佐藤"), rooms.map { it.title })
        assertEquals(listOf("new", "old", ""), rooms.map { it.lastMessage.orEmpty() })
    }
}

private class FakeOperationsStreamDataSource(
    private val points: List<OperationPoint> = emptyList(),
    private val staff: List<OperationStaff> = emptyList(),
    private val assignments: List<OperationAssignment> = emptyList(),
    private val instructions: List<OperationInstruction> = emptyList(),
    private val threads: List<OperationThread> = emptyList(),
    private val messages: List<OperationMessage> = emptyList(),
) : OperationsStreamDataSource {
    override fun observePoints(): Flow<List<OperationPoint>> = flowOf(points)

    override fun observeStaff(): Flow<List<OperationStaff>> = flowOf(staff)

    override fun observeAssignments(): Flow<List<OperationAssignment>> = flowOf(assignments)

    override fun observeInstructions(): Flow<List<OperationInstruction>> = flowOf(instructions)

    override fun observeThreads(): Flow<List<OperationThread>> = flowOf(threads)

    override fun observeMessages(): Flow<List<OperationMessage>> = flowOf(messages)

    override fun start() = Unit

    override fun stop() = Unit
}
