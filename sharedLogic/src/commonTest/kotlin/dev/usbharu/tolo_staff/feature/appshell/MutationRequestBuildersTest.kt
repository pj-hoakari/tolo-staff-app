package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo.communication.v1.ReportCategory
import dev.usbharu.tolo.communication.v1.Severity
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MutationRequestBuildersTest {
    private val session = AppSessionContext("tenant-1", "event-1", "staff-1")

    @Test
    fun `activate assignment includes event scope id and expected version`() {
        val request = activateAssignmentRequest(session, "assignment-1", 12)

        assertEquals("tenant-1", request.scope.tenantId.value)
        assertEquals("event-1", request.scope.eventId.value)
        assertEquals("assignment-1", request.assignmentId.value)
        assertEquals(12, request.expectedVersion)
    }

    @Test
    fun `submit report includes category severity point timestamp and request id`() {
        val request = submitReportRequest(
            session = session,
            requestId = "request-1",
            category = ReportCategoryValue.SAFETY_HAZARD,
            severity = ReportSeverityValue.URGENT,
            pointId = "point-1",
            description = "  危険箇所です  ",
            observedAt = Instant.parse("2026-07-13T01:02:03Z"),
        )

        assertEquals("request-1", request.requestId)
        assertEquals(ReportCategory.REPORT_CATEGORY_SAFETY_HAZARD, request.category)
        assertEquals(Severity.SEVERITY_URGENT, request.severity)
        assertEquals("point-1", request.pointId.value)
        assertEquals("危険箇所です", request.description)
        assertEquals(
            Instant.parse("2026-07-13T01:02:03Z"),
            Instant.fromEpochSeconds(request.observedAt.seconds, request.observedAt.nanos),
        )
    }
}
