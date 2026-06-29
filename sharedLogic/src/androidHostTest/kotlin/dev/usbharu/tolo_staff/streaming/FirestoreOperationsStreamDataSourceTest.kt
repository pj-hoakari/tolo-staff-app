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

    @Test
    fun `fallback decoder accepts nested firestore sender fields`() {
        val fields = mapOf(
            "message.created_at" to "2026-06-19T09:00:00Z",
            "message.thread_id" to "thread-1",
            "message.message_id" to "message-1",
            "message.staff_id" to "tanaka",
            "sender.name" to "田中",
            "message.simple" to "present",
            "payload.simple.text" to "こちら配置済みです",
        )

        val message = decodeMessageDocumentFields(
            documentId = "doc-1",
            optionalString = { aliases -> aliases.firstNotNullOfOrNull(fields::get) },
            contains = fields::containsKey,
        ).toOperationMessage()

        assertEquals("message-1", message.messageId)
        assertEquals("thread-1", message.threadId)
        assertEquals("tanaka", message.staffId)
        assertEquals(OperationMessageType.SIMPLE, message.messageType)
        assertEquals("こちら配置済みです", message.text)
        assertEquals("田中", message.senderName)
    }

    @Test
    fun `fallback decoder uses sender envelope when top level sender fields are absent`() {
        val fields = mapOf(
            "message.created_at" to "2026-06-19T09:00:00Z",
            "message.thread_id" to "thread-2",
            "message.message_id" to "message-2",
            "message.simple" to "present",
            "payload.simple.text" to "巡回開始します",
        )

        val message = decodeMessageDocumentFields(
            documentId = "doc-2",
            optionalString = { aliases -> aliases.firstNotNullOfOrNull(fields::get) },
            contains = fields::containsKey,
            senderStaffIdFallback = "sato",
            senderNameFallback = "佐藤",
        ).toOperationMessage()

        assertEquals("message-2", message.messageId)
        assertEquals("thread-2", message.threadId)
        assertEquals("sato", message.staffId)
        assertEquals("佐藤", message.senderName)
        assertEquals(OperationMessageType.SIMPLE, message.messageType)
        assertEquals("巡回開始します", message.text)
    }
}
