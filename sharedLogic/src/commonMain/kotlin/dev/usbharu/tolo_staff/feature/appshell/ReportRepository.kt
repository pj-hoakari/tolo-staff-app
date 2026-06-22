package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateReportRequest
import dev.usbharu.tolo.communication.grpc.Report
import dev.usbharu.tolo.communication.grpc.ReportPriority
import dev.usbharu.tolo.communication.grpc.StaffIdRequest
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.toIsoString
import kotlin.random.Random

interface ReportRepository {
    suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport>

    suspend fun submitReport(
        currentStaffId: String,
        title: String,
        summary: String,
        priorityLabel: String,
    ): SubmittedReport
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

data class SubmittedReport(
    val reportId: String,
    val threadId: String,
    val createdAtLabel: String? = null,
)

class NoOpReportRepository : ReportRepository {
    override suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport> = emptyList()

    override suspend fun submitReport(
        currentStaffId: String,
        title: String,
        summary: String,
        priorityLabel: String,
    ): SubmittedReport = error("Report submission is not available")
}

class GrpcReportRepository(
    private val fetchRelevantReports: suspend (String) -> List<RelevantReport>,
    private val submitReportRequest: suspend (String, String, String, String) -> SubmittedReport,
) : ReportRepository {
    private val logger = AppLogger.withTag("GrpcReportRepository")

    constructor(grpcClient: GrpcCommunicationClient) : this(
        fetchRelevantReports = { currentStaffId ->
            grpcClient.reportService.ListRelevantReports(
                StaffIdRequest {
                    staffId = currentStaffId
                }
            ).reports.map { it.toRelevantReport() }
        },
        submitReportRequest = { currentStaffId, title, summary, priorityLabel ->
            grpcClient.reportService.CreateReport(
                CreateReportRequest {
                    report = Report {
                        reportId = buildReportId(currentStaffId)
                        staffId = currentStaffId
                        this.title = title
                        description = summary
                        priority = priorityLabel.toReportPriority()
                    }
                }
            ).toSubmittedReport()
        }
    )

    override suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport> =
        fetchRelevantReports(currentStaffId)
            .also {
                logger.debug {
                    "Fetched relevant reports via gRPC: currentStaffId=$currentStaffId, count=${it.size}"
                }
            }

    override suspend fun submitReport(
        currentStaffId: String,
        title: String,
        summary: String,
        priorityLabel: String,
    ): SubmittedReport =
        submitReportRequest(currentStaffId, title, summary, priorityLabel)
            .also {
                logger.debug {
                    "Submitted report via gRPC: currentStaffId=$currentStaffId, reportId=${it.reportId}, threadId=${it.threadId}"
                }
            }
}

private fun Report.toRelevantReport(): RelevantReport = RelevantReport(
    reportId = reportId,
    threadId = threadId.requireNotBlank("threadId"),
    authorStaffId = staffId,
    title = title.ifBlank { "報告" },
    summary = description,
    priorityLabel = priority.toLabel(),
    createdAtLabel = createdAt.toIsoString().ifBlank { null },
)

private fun Report.toSubmittedReport(): SubmittedReport = SubmittedReport(
    reportId = reportId.requireNotBlank("reportId"),
    threadId = threadId.requireNotBlank("threadId"),
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

private fun String.toReportPriority(): ReportPriority =
    when (this) {
        "低" -> ReportPriority.REPORT_PRIORITY_LOW
        "通常" -> ReportPriority.REPORT_PRIORITY_MEDIUM
        "高" -> ReportPriority.REPORT_PRIORITY_HIGH
        "緊急" -> ReportPriority.REPORT_PRIORITY_CRITICAL
        else -> ReportPriority.REPORT_PRIORITY_UNSPECIFIED
    }

private fun String.requireNotBlank(fieldName: String): String =
    takeIf { it.isNotBlank() } ?: error("Report $fieldName is blank")

private fun buildReportId(currentStaffId: String): String =
    "client-report-$currentStaffId-${Random.nextLong().toString().replace('-', '0')}"
