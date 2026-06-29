package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateReportRequest
import dev.usbharu.tolo.communication.grpc.ListRelevantReportsRequest
import dev.usbharu.tolo.communication.grpc.PageRequest
import dev.usbharu.tolo.communication.grpc.Report
import dev.usbharu.tolo.communication.grpc.ReportPriority
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.NoOpOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationEntityType
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.toIsoString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ReportRepository {
    fun observeRelevantReports(currentStaffId: String): Flow<List<RelevantReport>>

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
    override fun observeRelevantReports(currentStaffId: String): Flow<List<RelevantReport>> = flow { emit(emptyList()) }

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
    private val changeNotifier: OperationsChangeNotifier = NoOpOperationsChangeNotifier(),
) : ReportRepository {
    private val logger = AppLogger.withTag("GrpcReportRepository")

    constructor(
        grpcClient: GrpcCommunicationClient,
        changeNotifier: OperationsChangeNotifier = NoOpOperationsChangeNotifier(),
    ) : this(
        fetchRelevantReports = { currentStaffId ->
            grpcClient.reportService.ListRelevantReports(
                ListRelevantReportsRequest {
                    staffId = currentStaffId
                    page = PageRequest {
                        pageSize = DEFAULT_PAGE_SIZE
                    }
                }
            ).reports.map { it.toRelevantReport() }
        },
        submitReportRequest = { currentStaffId, title, summary, priorityLabel ->
            grpcClient.reportService.CreateReport(
                buildCreateReportRequest(
                    currentStaffId = currentStaffId,
                    title = title,
                    summary = summary,
                    priorityLabel = priorityLabel,
                )
            ).toSubmittedReport()
        },
        changeNotifier = changeNotifier,
    )

    override fun observeRelevantReports(currentStaffId: String): Flow<List<RelevantReport>> = flow {
        emit(listRelevantReports(currentStaffId))
        changeNotifier.observeStaff(currentStaffId).collect { change ->
            if (change.entityType == OperationEntityType.REPORT) {
                emit(listRelevantReports(currentStaffId))
            }
        }
    }

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

internal fun buildCreateReportRequest(
    currentStaffId: String,
    title: String,
    summary: String,
    priorityLabel: String,
): CreateReportRequest = CreateReportRequest {
    report = Report {
        staffId = currentStaffId
        this.title = title
        description = summary
        priority = priorityLabel.toReportPriority()
    }
}

private const val DEFAULT_PAGE_SIZE = 50
