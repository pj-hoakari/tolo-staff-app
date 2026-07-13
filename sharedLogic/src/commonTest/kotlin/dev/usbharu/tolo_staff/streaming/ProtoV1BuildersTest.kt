package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant

class ProtoV1BuildersTest {
    @Test
    fun `event scope and identifiers preserve their values`() {
        val scope = AppSessionContext("tenant-1", "event-1", "staff-1").toProtoScope()

        assertEquals("tenant-1", scope.tenantId.value)
        assertEquals("event-1", scope.eventId.value)
        assertEquals("staff-1", "staff-1".toProtoStaffId().value)
        assertEquals("point-1", "point-1".toProtoPointId().value)
        assertEquals("assignment-1", "assignment-1".toProtoAssignmentId().value)
    }

    @Test
    fun `page request and timestamp preserve paging and time`() {
        val page = pageRequest(50, "next")
        val instant = Instant.parse("2026-07-13T01:02:03.456Z")

        assertEquals(50, page.pageSize)
        assertEquals("next", page.pageToken)
        assertEquals(instant, Instant.parse(instant.toProtoTimestamp().toIsoStringOrEmpty()))
    }

    @Test
    fun `paginate fetches until the next token is blank`() = runTest {
        val requestedTokens = mutableListOf<String>()

        val result = paginate { token ->
            requestedTokens += token
            when (token) {
                "" -> PagedResult(listOf(1, 2), "page-2")
                "page-2" -> PagedResult(listOf(3), "")
                else -> error("unexpected token")
            }
        }

        assertEquals(listOf("", "page-2"), requestedTokens)
        assertEquals(listOf(1, 2, 3), result)
        assertTrue(result.isNotEmpty())
    }
}
