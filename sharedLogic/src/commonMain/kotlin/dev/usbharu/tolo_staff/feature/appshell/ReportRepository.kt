package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.NoOpOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationEntityType
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
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
