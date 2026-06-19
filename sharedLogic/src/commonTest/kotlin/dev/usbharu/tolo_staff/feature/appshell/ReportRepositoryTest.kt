package dev.usbharu.tolo_staff.feature.appshell

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReportRepositoryTest {
    @Test
    fun `grpc report repository returns fetched reports`() = runTest {
        val repository = GrpcReportRepository { currentStaffId ->
            listOf(
                RelevantReport(
                    reportId = "report-1",
                    threadId = "thread-1",
                    authorStaffId = currentStaffId,
                    title = "導線報告",
                    summary = "列整理を継続中",
                    priorityLabel = "高",
                    createdAtLabel = "2026-06-19T09:00:00Z",
                )
            )
        }

        val reports = repository.listRelevantReports("tanaka")

        assertEquals(1, reports.size)
        assertEquals("report-1", reports.first().reportId)
        assertEquals("tanaka", reports.first().authorStaffId)
    }

    @Test
    fun `grpc report repository surfaces fetch failures`() = runTest {
        val repository = GrpcReportRepository { _ ->
            error("network down")
        }

        val error = assertFailsWith<IllegalStateException> {
            repository.listRelevantReports("tanaka")
        }

        assertEquals("network down", error.message)
    }
}
