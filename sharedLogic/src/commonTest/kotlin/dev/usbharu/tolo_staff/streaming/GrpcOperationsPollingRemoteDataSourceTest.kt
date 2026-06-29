package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.SimpleMessagePayload
import dev.usbharu.tolo.communication.grpc.Thread
import dev.usbharu.tolo.communication.grpc.invoke
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcOperationsPollingRemoteDataSourceTest {
    @Test
    fun `thread mapping keeps proto display title`() {
        val thread = Thread {
            threadId = "thread-1"
            memberStaffIds = listOf("tanaka", "sato")
            displayTitle = "Aゲート連絡"
        }

        val mapped = thread.toOperationThread()

        assertEquals("Aゲート連絡", mapped.displayTitle)
        assertEquals(listOf("tanaka", "sato"), mapped.members)
    }

    @Test
    fun `message mapping uses created at as iso updatedAt`() {
        val message = Message {
            messageId = "message-1"
            threadId = "thread-1"
            staffId = "tanaka"
            payload = Message.Payload.Simple(
                SimpleMessagePayload {
                    text = "了解です"
                }
            )
            createdAt = Timestamp {
                seconds = 1_750_324_800L
                nanos = 123_000_000
            }
        }

        val mapped = message.toOperationMessage()

        assertEquals(Instant.fromEpochSeconds(1_750_324_800L, 123_000_000).toString(), mapped.updatedAt)
        assertEquals("了解です", mapped.text)
    }
}
