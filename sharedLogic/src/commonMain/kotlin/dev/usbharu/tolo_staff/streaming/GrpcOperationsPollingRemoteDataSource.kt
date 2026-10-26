package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlin.time.Instant
import tolo.kernel.v1.GraphAnchor
import tolo.operation.v1.Audience
import tolo.operation.v1.ListReassignmentsRequest
import tolo.operation.v1.ListStaffMessagesRequest
import tolo.operation.v1.Reassignment
import tolo.operation.v1.ReassignmentStatus
import tolo.operation.v1.StaffMessage
import tolo.operation.v1.invoke

data class PagedResult<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
)

class GrpcOperationsPollingRemoteDataSource(
    private val grpcClient: GrpcCommunicationClient,
    private val eventId: String = "",
    private val tenantId: String = "",
) : OperationsPollingRemoteDataSource {
    private val logger = AppLogger.withTag("GrpcOperationsPollingRemoteDataSource")

    override suspend fun listPoints(): List<OperationPoint> = emptyList()

    override suspend fun listStaff(): List<OperationStaff> =
        listAssignments()
            .map { it.staffId }
            .filter { it.isNotBlank() }
            .distinct()
            .map { staffId ->
                OperationStaff(
                    reason = "grpc.listReassignments",
                    entityId = staffId,
                    staffId = staffId,
                    name = staffId,
                )
            }
            .also { logger.debug { "Derived staff from reassignments: count=${it.size}" } }

    override suspend fun listAssignments(): List<OperationAssignment> =
        listAssignmentsPage().items
            .also { logger.debug { "Fetched assignments via gRPC: count=${it.size}" } }

    override suspend fun listInstructions(): List<OperationInstruction> =
        listStaffMessages()
            .map { message -> message.toOperationInstruction(staffIds = emptyList()) }
            .also { logger.debug { "Fetched instructions via gRPC: count=${it.size}" } }

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> =
        listStaffMessages()
            .map { message -> message.toOperationInstruction(staffIds = listOf(staffId)) }
            .also {
                logger.debug {
                    "Fetched relevant instructions via gRPC: staffId=$staffId, count=${it.size}"
                }
            }

    override suspend fun listThreads(): List<OperationThread> = emptyList()

    override suspend fun listMessages(): List<OperationMessage> = emptyList()

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> = emptyList()

    suspend fun listAssignmentsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationAssignment> {
        val reassignments = safeCall("ListReassignments") {
            grpcClient.operationControlService.ListReassignments(
                ListReassignmentsRequest {
                    this.eventId = this@GrpcOperationsPollingRemoteDataSource.eventId
                    this.tenantId = this@GrpcOperationsPollingRemoteDataSource.tenantId
                }
            ).reassignments
        }.orEmpty()
        return PagedResult(items = reassignments.map { it.toOperationAssignment() })
    }

    suspend fun listRelevantInstructionsPage(
        staffId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationInstruction> = PagedResult(
        items = listRelevantInstructions(staffId),
    )

    suspend fun listThreadsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationThread> = PagedResult(items = emptyList())

    suspend fun listMessagesPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationMessage> = PagedResult(items = emptyList())

    suspend fun listThreadMessagesPage(
        threadId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationMessage> = PagedResult(items = emptyList())

    private suspend fun listStaffMessages(): List<StaffMessage> =
        safeCall("ListStaffMessages") {
            grpcClient.staffCommunicationService.ListStaffMessages(
                ListStaffMessagesRequest {
                    this.eventId = this@GrpcOperationsPollingRemoteDataSource.eventId
                    this.tenantId = this@GrpcOperationsPollingRemoteDataSource.tenantId
                    audience = Audience.AUDIENCE_STAFF
                }
            ).messages
        }.orEmpty()

    private suspend fun <T> safeCall(
        operation: String,
        block: suspend () -> T,
    ): T? = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        logger.warn(throwable) { "gRPC call failed: operation=$operation" }
        null
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 100
    }
}

internal fun Reassignment.toOperationAssignment(): OperationAssignment = OperationAssignment(
    updatedAt = "",
    reason = "grpc.listReassignments",
    entityId = reassignmentId,
    assignId = reassignmentId,
    pointId = target.toPointId().orEmpty(),
    staffId = staffUserId,
    status = when (status) {
        ReassignmentStatus.REASSIGNMENT_STATUS_DIRECTED -> OperationAssignmentStatus.PENDING
        ReassignmentStatus.REASSIGNMENT_STATUS_APPLIED -> OperationAssignmentStatus.ACTIVE
        ReassignmentStatus.REASSIGNMENT_STATUS_UNSPECIFIED,
        is ReassignmentStatus.UNRECOGNIZED,
        -> OperationAssignmentStatus.PENDING
    },
)

internal fun Reassignment.Target?.toPointId(): String? = when (this) {
    is Reassignment.Target.ObservationPointId -> value
    is Reassignment.Target.GatePointId -> value
    is Reassignment.Target.Anchor -> when (val anchorTarget = value.target) {
        is GraphAnchor.Target.PointId -> anchorTarget.value
        is GraphAnchor.Target.RouteId -> null
        null -> null
    }

    null -> null
}

internal fun StaffMessage.toOperationInstruction(staffIds: List<String>): OperationInstruction {
    val content = body.trim().ifBlank { DEFAULT_INSTRUCTION_TITLE }
    val lines = content.lines()
    val hasMultipleLines = lines.size > 1
    val title = if (hasMultipleLines) {
        lines.first().trim().ifBlank { DEFAULT_INSTRUCTION_TITLE }
    } else {
        DEFAULT_INSTRUCTION_TITLE
    }
    val description = if (hasMultipleLines) {
        lines.drop(1).joinToString("\n").trim()
    } else {
        content
    }
    return OperationInstruction(
        updatedAt = sentAt.toIsoString(),
        reason = "grpc.listStaffMessages",
        entityId = messageId,
        instructionId = messageId,
        pointIds = emptyList(),
        staffIds = staffIds,
        title = title,
        description = description,
        status = OperationInstructionStatus.ACTIVE,
        threadId = null,
    )
}

internal fun Timestamp?.toIsoString(): String = this
    ?.let { Instant.fromEpochSeconds(it.seconds, it.nanos).toString() }
    .orEmpty()

private const val DEFAULT_INSTRUCTION_TITLE = "本部からの連絡"
