package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreChangeHeadPathTest {
    private val session = AppSessionContext("tenant-1", "event-1", "staff-1")

    @Test
    fun `global change head is nested under tenant and event`() {
        assertEquals(
            "tenants/tenant-1/events/event-1/operations_global_change_heads/global",
            eventScopedChangeHeadPath(session, "operations_global_change_heads", "global"),
        )
    }

    @Test
    fun `staff change head is nested under tenant and event`() {
        assertEquals(
            "tenants/tenant-1/events/event-1/operations_change_heads/staff-1",
            eventScopedChangeHeadPath(session, "operations_change_heads", session.staffId),
        )
    }
}
