package dev.usbharu.tolo_staff.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppRuntimeConfigTest {
    @Test
    fun `valid runtime config creates fixed session context`() {
        val config = AppRuntimeConfig(
            tenantId = "tenant-1",
            eventId = "event-1",
            staffId = "staff-1",
            grpcHost = "localhost",
            grpcPort = 8080,
        ).requireValid()

        assertEquals(AppSessionContext("tenant-1", "event-1", "staff-1"), config.toSessionContext())
    }

    @Test
    fun `blank identifiers and invalid port are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig("", "event", "staff", "localhost").requireValid()
        }
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig("tenant", "event", "staff", "localhost", 0).requireValid()
        }
    }

    @Test
    fun `request ids are non blank and distinct`() {
        val first = DefaultRequestIdGenerator.nextId()
        val second = DefaultRequestIdGenerator.nextId()

        assertTrue(first.startsWith("tolo-"))
        assertTrue(second.startsWith("tolo-"))
        assertTrue(first != second)
    }
}
