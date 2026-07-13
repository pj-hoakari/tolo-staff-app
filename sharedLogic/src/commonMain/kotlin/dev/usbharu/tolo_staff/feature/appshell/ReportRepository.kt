package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo.communication.v1.FieldReport
import dev.usbharu.tolo.communication.v1.ListReportsRequest
import dev.usbharu.tolo.communication.v1.ReportCategory
import dev.usbharu.tolo.communication.v1.ReportStatus
import dev.usbharu.tolo.communication.v1.Severity
import dev.usbharu.tolo.communication.v1.SubmitReportRequest
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.DefaultRequestIdGenerator
import dev.usbharu.tolo_staff.runtime.RequestIdGenerator
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.NoOpOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.PagedResult
import dev.usbharu.tolo_staff.streaming.pageRequest
import dev.usbharu.tolo_staff.streaming.paginate
import dev.usbharu.tolo_staff.streaming.toIsoStringOrEmpty
import dev.usbharu.tolo_staff.streaming.toProtoPointId
import dev.usbharu.tolo_staff.streaming.toProtoScope
import dev.usbharu.tolo_staff.streaming.toProtoTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlin.time.Instant

enum class ReportCategoryValue { CONGESTION, SAFETY_HAZARD, EQUIPMENT, GATE, OTHER }

enum class ReportSeverityValue { INFO, ATTENTION, URGENT }

data class RelevantReport(
    val reportId: String,
    val authorStaffId: String,
    val pointId: String?,
    val category: ReportCategoryValue,
    val severity: ReportSeverityValue,
    val statusLabel: String,
    val summary: String,
    val createdAtLabel: String? = null,
)

interface ReportRepository {
    fun observeRelevantReports(
        currentStaffId: String,
        assignedPointIds: Set<String> = emptySet(),
    ): Flow<List<RelevantReport>>

    suspend fun listRelevantReports(
        currentStaffId: String,
        assignedPointIds: Set<String> = emptySet(),
    ): List<RelevantReport>

    suspend fun submit(
        category: ReportCategoryValue,
        severity: ReportSeverityValue,
        pointId: String,
        description: String,
    ): RelevantReport
}

class NoOpReportRepository : ReportRepository {
    override fun observeRelevantReports(currentStaffId: String, assignedPointIds: Set<String>) =
        flow<List<RelevantReport>> { emit(emptyList()) }

    override suspend fun listRelevantReports(currentStaffId: String, assignedPointIds: Set<String>) =
        emptyList<RelevantReport>()

    override suspend fun submit(
        category: ReportCategoryValue,
        severity: ReportSeverityValue,
        pointId: String,
        description: String,
    ): RelevantReport = error("Report submission is not available")
}

class GrpcReportRepository(
    private val grpcClient: GrpcCommunicationClient,
    private val session: AppSessionContext,
    private val changeNotifier: OperationsChangeNotifier = NoOpOperationsChangeNotifier(),
    private val pollingIntervalMillis: Long? = null,
    private val requestIdGenerator: RequestIdGenerator = DefaultRequestIdGenerator,
    private val now: () -> Instant = { Clock.System.now() },
) : ReportRepository {
    private val logger = AppLogger.withTag("GrpcReportRepository")

    override fun observeRelevantReports(
        currentStaffId: String,
        assignedPointIds: Set<String>,
    ): Flow<List<RelevantReport>> = flow {
        var hasEmitted = false

        suspend fun refresh() {
            val reports = try {
                listRelevantReports(currentStaffId, assignedPointIds)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logger.warn(throwable) {
                    "Failed to refresh reports; keeping the observer active for retry"
                }
                null
            }
            if (reports != null) {
                emit(reports)
                hasEmitted = true
            } else if (!hasEmitted) {
                emit(emptyList())
                hasEmitted = true
            }
        }

        refresh()
        val interval = pollingIntervalMillis
        if (interval != null) {
            while (currentCoroutineContext().isActive) {
                delay(interval)
                refresh()
            }
        } else {
            changeNotifier.observeStaff(currentStaffId).collect {
                refresh()
            }
        }
    }

    override suspend fun listRelevantReports(
        currentStaffId: String,
        assignedPointIds: Set<String>,
    ): List<RelevantReport> = paginate { token ->
        val response = grpcClient.reportService.ListReports(
            ListReportsRequest {
                scope = session.toProtoScope()
                page = pageRequest(DEFAULT_PAGE_SIZE, token)
            }
        )
        PagedResult(
            response.reports.map(FieldReport::toRelevantReport),
            response.page.nextPageToken.ifBlank { null },
        )
    }.filter { it.authorStaffId == currentStaffId || it.pointId in assignedPointIds }

    override suspend fun submit(
        category: ReportCategoryValue,
        severity: ReportSeverityValue,
        pointId: String,
        description: String,
    ): RelevantReport = grpcClient.reportService.SubmitReport(
        submitReportRequest(
            session = session,
            requestId = requestIdGenerator.nextId(),
            category = category,
            severity = severity,
            pointId = pointId,
            description = description,
            observedAt = now(),
        )
    ).toRelevantReport()

    private companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_DESCRIPTION_LENGTH = 2_000
    }
}

internal fun submitReportRequest(
    session: AppSessionContext,
    requestId: String,
    category: ReportCategoryValue,
    severity: ReportSeverityValue,
    pointId: String,
    description: String,
    observedAt: Instant,
): SubmitReportRequest = SubmitReportRequest {
    scope = session.toProtoScope()
    this.requestId = requestId
    this.category = category.toProto()
    this.severity = severity.toProto()
    this.pointId = pointId.toProtoPointId()
    this.description = description.trim().take(2_000)
    this.observedAt = observedAt.toProtoTimestamp()
}

internal fun FieldReport.toRelevantReport(): RelevantReport = RelevantReport(
    reportId = id.value,
    authorStaffId = reporterStaffId.value,
    pointId = pointId.value.ifBlank { null },
    category = category.toValue(),
    severity = severity.toValue(),
    statusLabel = status.toLabel(),
    summary = description,
    createdAtLabel = createdAt.toIsoStringOrEmpty().ifBlank { null },
)

internal fun ReportCategoryValue.toProto(): ReportCategory = when (this) {
    ReportCategoryValue.CONGESTION -> ReportCategory.REPORT_CATEGORY_CONGESTION
    ReportCategoryValue.SAFETY_HAZARD -> ReportCategory.REPORT_CATEGORY_SAFETY_HAZARD
    ReportCategoryValue.EQUIPMENT -> ReportCategory.REPORT_CATEGORY_EQUIPMENT
    ReportCategoryValue.GATE -> ReportCategory.REPORT_CATEGORY_GATE
    ReportCategoryValue.OTHER -> ReportCategory.REPORT_CATEGORY_OTHER
}

internal fun ReportCategory.toValue(): ReportCategoryValue = when (this) {
    ReportCategory.REPORT_CATEGORY_CONGESTION -> ReportCategoryValue.CONGESTION
    ReportCategory.REPORT_CATEGORY_SAFETY_HAZARD -> ReportCategoryValue.SAFETY_HAZARD
    ReportCategory.REPORT_CATEGORY_EQUIPMENT -> ReportCategoryValue.EQUIPMENT
    ReportCategory.REPORT_CATEGORY_GATE -> ReportCategoryValue.GATE
    ReportCategory.REPORT_CATEGORY_OTHER,
    ReportCategory.REPORT_CATEGORY_UNSPECIFIED,
    is ReportCategory.UNRECOGNIZED,
    -> ReportCategoryValue.OTHER
}

internal fun ReportSeverityValue.toProto(): Severity = when (this) {
    ReportSeverityValue.INFO -> Severity.SEVERITY_INFO
    ReportSeverityValue.ATTENTION -> Severity.SEVERITY_ATTENTION
    ReportSeverityValue.URGENT -> Severity.SEVERITY_URGENT
}

internal fun Severity.toValue(): ReportSeverityValue = when (this) {
    Severity.SEVERITY_ATTENTION -> ReportSeverityValue.ATTENTION
    Severity.SEVERITY_URGENT -> ReportSeverityValue.URGENT
    Severity.SEVERITY_INFO,
    Severity.SEVERITY_UNSPECIFIED,
    is Severity.UNRECOGNIZED,
    -> ReportSeverityValue.INFO
}

internal fun ReportStatus.toLabel(): String = when (this) {
    ReportStatus.REPORT_STATUS_OPEN -> "未対応"
    ReportStatus.REPORT_STATUS_ACKNOWLEDGED -> "確認済み"
    ReportStatus.REPORT_STATUS_RESOLVED -> "解決済み"
    ReportStatus.REPORT_STATUS_DISMISSED -> "却下"
    ReportStatus.REPORT_STATUS_UNSPECIFIED,
    is ReportStatus.UNRECOGNIZED,
    -> ""
}

internal fun ReportCategoryValue.toLabel(): String = when (this) {
    ReportCategoryValue.CONGESTION -> "混雑"
    ReportCategoryValue.SAFETY_HAZARD -> "安全上の危険"
    ReportCategoryValue.EQUIPMENT -> "設備"
    ReportCategoryValue.GATE -> "ゲート"
    ReportCategoryValue.OTHER -> "その他"
}

internal fun ReportSeverityValue.toLabel(): String = when (this) {
    ReportSeverityValue.INFO -> "通常"
    ReportSeverityValue.ATTENTION -> "高"
    ReportSeverityValue.URGENT -> "緊急"
}
