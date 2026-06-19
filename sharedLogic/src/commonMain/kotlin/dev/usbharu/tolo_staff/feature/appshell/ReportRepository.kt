package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.Report
import dev.usbharu.tolo.communication.grpc.ReportPriority
import dev.usbharu.tolo.communication.grpc.StaffIdRequest
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.toIsoString

interface ReportRepository {
    suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport>
}

data class RelevantReport(
    val reportId: String,
    val threadId: String,
    val authorStaffId: String,
    val title: String,
    val summary: String,
    val priorityLabel: String,
    val createdAtLabel: String? = null,
)

class NoOpReportRepository : ReportRepository {
    override suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport> = emptyList()
}

class GrpcReportRepository(
    private val fetchRelevantReports: suspend (String) -> List<RelevantReport>
) : ReportRepository {
    private val logger = AppLogger.withTag("GrpcReportRepository")

    constructor(grpcClient: GrpcCommunicationClient) : this(
        fetchRelevantReports = { currentStaffId ->
            grpcClient.reportService.ListRelevantReports(
                StaffIdRequest {
                    staffId = currentStaffId
                }
            ).reports.map { it.toRelevantReport() }
        }
    )

    override suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport> =
        fetchRelevantReports(currentStaffId)
            .also {
                logger.debug {
                    "Fetched relevant reports via gRPC: currentStaffId=$currentStaffId, count=${it.size}"
                }
            }
}

private fun Report.toRelevantReport(): RelevantReport = RelevantReport(
    reportId = reportId,
    threadId = threadId.ifBlank { "report-$reportId" },
    authorStaffId = staffId,
    title = title.ifBlank { "報告" },
    summary = description,
    priorityLabel = priority.toLabel(),
    createdAtLabel = createdAt.toIsoString().ifBlank { null },
)

private fun ReportPriority.toLabel(): String =
    when (this) {
        ReportPriority.REPORT_PRIORITY_LOW -> "低"
        ReportPriority.REPORT_PRIORITY_MEDIUM -> "通常"
        ReportPriority.REPORT_PRIORITY_HIGH -> "高"
        ReportPriority.REPORT_PRIORITY_CRITICAL -> "緊急"
        ReportPriority.REPORT_PRIORITY_UNSPECIFIED -> ""
        is ReportPriority.UNRECOGNIZED -> ""
    }
