package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo.communication.v1.MessageAudience
import dev.usbharu.tolo.communication.v1.ThreadKind
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactMutationRequestBuildersTest {
    private val session = AppSessionContext("tenant-1", "event-1", "staff-1")

    @Test
    fun `send message omits sender staff and uses authenticated staff audience`() {
        val request = sendMessageRequest(session, "request-1", "thread-1", "本文")

        assertEquals("request-1", request.requestId)
        assertEquals("thread-1", request.threadId.value)
        assertEquals(MessageAudience.MESSAGE_AUDIENCE_STAFF, request.audience)
        assertEquals("本文", request.body)
    }

    @Test
    fun `direct thread includes current and target staff`() {
        val request = createThreadRequest(
            session,
            "request-2",
            ChatThreadTarget("staff-2", "佐藤", ChatThreadTargetKind.DIRECT),
        )

        assertEquals(ThreadKind.THREAD_KIND_DIRECT, request.kind)
        assertEquals(listOf("staff-1", "staff-2"), request.participantStaffIds.map { it.value })
        assertEquals("", request.pointId.value)
    }

    @Test
    fun `location thread uses point and contains no role or headquarters semantics`() {
        val request = createThreadRequest(
            session,
            "request-3",
            ChatThreadTarget("point-1", "南口", ChatThreadTargetKind.LOCATION),
        )

        assertEquals(ThreadKind.THREAD_KIND_LOCATION, request.kind)
        assertEquals("point-1", request.pointId.value)
        assertEquals(listOf("staff-1"), request.participantStaffIds.map { it.value })
    }
}
