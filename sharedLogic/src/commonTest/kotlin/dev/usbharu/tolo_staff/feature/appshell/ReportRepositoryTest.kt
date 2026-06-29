package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo.communication.grpc.ReportPriority
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReportRepositoryTest {
    @Test
    fun `grpc report repository returns fetched reports`() = runTest {
        val repository = GrpcReportRepository(
            { currentStaffId ->
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
            },
            { _, _, _, _ -> SubmittedReport(reportId = "report-1", threadId = "thread-1") }
        )

        val reports = repository.listRelevantReports("tanaka")

        assertEquals(1, reports.size)
        assertEquals("report-1", reports.first().reportId)
        assertEquals("tanaka", reports.first().authorStaffId)
    }

    @Test
    fun `grpc report repository surfaces fetch failures`() = runTest {
        val repository = GrpcReportRepository(
            { _ ->
                error("network down")
            },
            { _, _, _, _ -> SubmittedReport(reportId = "report-1", threadId = "thread-1") }
        )

        val error = assertFailsWith<IllegalStateException> {
            repository.listRelevantReports("tanaka")
        }

        assertEquals("network down", error.message)
    }

    @Test
    fun `grpc report repository returns submitted report ids`() = runTest {
        val repository = GrpcReportRepository(
            { emptyList() },
            { currentStaffId, title, summary, priorityLabel ->
                assertEquals("tanaka", currentStaffId)
                assertEquals("導線報告", title)
                assertEquals("列整理を継続中", summary)
                assertEquals("高", priorityLabel)
                SubmittedReport(
                    reportId = "report-created-1",
                    threadId = "thread-created-1",
                    createdAtLabel = "2026-06-22T00:00:00Z",
                )
            }
        )

        val report = repository.submitReport("tanaka", "導線報告", "列整理を継続中", "高")

        assertEquals("report-created-1", report.reportId)
        assertEquals("thread-created-1", report.threadId)
        assertEquals("2026-06-22T00:00:00Z", report.createdAtLabel)
    }

    @Test
    fun `create report request leaves server owned ids unset`() {
        val request = buildCreateReportRequest(
            currentStaffId = "tanaka",
            title = "導線報告",
            summary = "列整理を継続中",
            priorityLabel = "高",
        )

        assertEquals("", request.report.reportId)
        assertEquals("", request.report.threadId)
        assertEquals("tanaka", request.report.staffId)
        assertEquals("導線報告", request.report.title)
        assertEquals("列整理を継続中", request.report.description)
        assertEquals(ReportPriority.REPORT_PRIORITY_HIGH, request.report.priority)
    }

    @Test
    fun `grpc report repository surfaces submission failures`() = runTest {
        val repository = GrpcReportRepository(
            { emptyList() },
            { _, _, _, _ -> error("submit failed") }
        )

        val error = assertFailsWith<IllegalStateException> {
            repository.submitReport("tanaka", "導線報告", "列整理を継続中", "高")
        }

        assertEquals("submit failed", error.message)
    }
}
