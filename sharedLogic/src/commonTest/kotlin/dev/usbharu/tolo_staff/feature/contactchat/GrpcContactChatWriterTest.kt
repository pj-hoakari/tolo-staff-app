package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GrpcContactChatWriterTest {
    @Test
    fun `direct chat messaging is disabled for the operation API`() = runTest {
        val writer = GrpcContactChatWriter()

        assertFailsWith<UnsupportedOperationException> {
            writer.sendSimpleMessage(
                roomId = "thread-1",
                currentStaffId = "tanaka",
                text = "配置につきました",
            )
        }
    }
}
