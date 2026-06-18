package dev.usbharu.tolo_staff.streaming

import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreOperationsStreamDataSourceTest {

    @Test
    fun `message type parser accepts lowercase firestore values`() {
        assertEquals(OperationMessageType.SIMPLE, OperationMessageType.parse("simple"))
        assertEquals(OperationMessageType.INSTRUCTION, OperationMessageType.parse("instruction"))
    }

    @Test
    fun `fallback decoded fields map to operation message`() {
        val message = OperationMessageDecodedFields(
            updatedAt = "2026-06-18T09:00:00Z",
            reason = "firestore.sync",
            entityId = "message-1",
            messageId = "message-1",
            threadId = "thread-1",
            staffId = "sato",
            messageType = OperationMessageType.SIMPLE,
            text = "了解です",
            replyTo = "message-0",
        ).toOperationMessage()

        assertEquals("message-1", message.messageId)
        assertEquals("thread-1", message.threadId)
        assertEquals("sato", message.staffId)
        assertEquals(OperationMessageType.SIMPLE, message.messageType)
        assertEquals("了解です", message.text)
        assertEquals("message-0", message.replyTo)
    }
}
