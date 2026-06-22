package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactChatMappingsTest {
    @Test
    fun `derive title prefers proto display title`() {
        val thread = OperationThread(
            updatedAt = "",
            reason = "test",
            entityId = "thread-1",
            threadId = "thread-1",
            members = listOf("tanaka", "sato"),
            displayTitle = "Aゲート連絡",
        )

        val title = thread.deriveTitle(
            currentStaffId = "tanaka",
            staff = listOf(
                OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤")
            ),
        )

        assertEquals("Aゲート連絡", title)
    }

    @Test
    fun `derive title falls back to member names when proto display title is blank`() {
        val thread = OperationThread(
            updatedAt = "",
            reason = "test",
            entityId = "thread-1",
            threadId = "thread-1",
            members = listOf("tanaka", "sato", "suzuki"),
            displayTitle = "",
        )

        val title = thread.deriveTitle(
            currentStaffId = "tanaka",
            staff = listOf(
                OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤"),
                OperationStaff(updatedAt = "", reason = "test", entityId = "suzuki", staffId = "suzuki", name = "鈴木"),
            ),
        )

        assertEquals("佐藤, 鈴木", title)
    }

    @Test
    fun `threads are sorted by latest message and threads without messages stay last`() {
        val threads = listOf(
            OperationThread(updatedAt = "", reason = "test", entityId = "thread-old", threadId = "thread-old"),
            OperationThread(updatedAt = "", reason = "test", entityId = "thread-none", threadId = "thread-none"),
            OperationThread(updatedAt = "", reason = "test", entityId = "thread-new", threadId = "thread-new"),
        )
        val messages = listOf(
            operationMessage(threadId = "thread-old", messageId = "m-1", updatedAt = "2026-06-19T09:00:00Z", text = "old"),
            operationMessage(threadId = "thread-new", messageId = "m-2", updatedAt = "2026-06-19T10:00:00Z", text = "new"),
        )

        val sorted = threads.sortedByLatestMessage(messages)

        assertEquals(listOf("thread-new", "thread-old", "thread-none"), sorted.map { it.threadId })
        assertEquals(mapOf("thread-old" to "old", "thread-new" to "new"), messages.toRoomPreviewByThread())
    }

    @Test
    fun `ui message falls back to sender name when sender id is unavailable`() {
        val message = OperationMessage(
            updatedAt = "2026-06-19T09:00:00Z",
            reason = "test",
            entityId = "message-1",
            messageId = "message-1",
            threadId = "thread-1",
            staffId = "null",
            messageType = OperationMessageType.SIMPLE,
            text = "了解です",
            senderName = "佐藤",
        ).toUiMessage(
            currentStaffId = "tanaka",
            staff = listOf(
                OperationStaff(updatedAt = "", reason = "test", entityId = "tanaka", staffId = "tanaka", name = "田中"),
                OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤"),
            ),
        )

        assertEquals("佐藤", message.senderName)
        assertEquals(false, message.isFromCurrentUser)
    }

    @Test
    fun `ui message does not identify current user from sender name when sender id is unavailable`() {
        val message = OperationMessage(
            updatedAt = "2026-06-19T09:00:00Z",
            reason = "test",
            entityId = "message-1",
            messageId = "message-1",
            threadId = "thread-1",
            staffId = "null",
            messageType = OperationMessageType.SIMPLE,
            text = "こちら配置済みです",
            senderName = "田中",
        ).toUiMessage(
            currentStaffId = "tanaka",
            staff = listOf(
                OperationStaff(updatedAt = "", reason = "test", entityId = "tanaka", staffId = "tanaka", name = "田中"),
                OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤"),
            ),
        )

        assertEquals("田中", message.senderName)
        assertEquals(false, message.isFromCurrentUser)
    }

}

private fun operationMessage(
    threadId: String,
    messageId: String,
    updatedAt: String,
    text: String,
) = OperationMessage(
    updatedAt = updatedAt,
    reason = "test",
    entityId = messageId,
    messageId = messageId,
    threadId = threadId,
    staffId = "tanaka",
    messageType = OperationMessageType.SIMPLE,
    text = text,
)
