package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo.communication.grpc.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcContactChatWriterTest {
    @Test
    fun `create message request leaves server owned message id unset`() {
        val request = buildCreateSimpleMessageRequest(
            roomId = "thread-1",
            currentStaffId = "tanaka",
            text = "配置につきました",
        )

        assertEquals("", request.message.messageId)
        assertEquals("thread-1", request.message.threadId)
        assertEquals("tanaka", request.message.staffId)
        val payload = request.message.payload as Message.Payload.Simple
        assertEquals("配置につきました", payload.value.text)
    }
}
