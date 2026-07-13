package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import dev.usbharu.tolo.communication.v1.AssignmentStatus
import dev.usbharu.tolo.communication.v1.ConversationThread
import dev.usbharu.tolo.communication.v1.Instruction
import dev.usbharu.tolo.communication.v1.InstructionStatus
import dev.usbharu.tolo.communication.v1.InstructionTarget
import dev.usbharu.tolo.communication.v1.ListAssignmentsRequest
import dev.usbharu.tolo.communication.v1.ListInstructionsRequest
import dev.usbharu.tolo.communication.v1.ListMessagesRequest
import dev.usbharu.tolo.communication.v1.ListPointsRequest
import dev.usbharu.tolo.communication.v1.ListStaffRequest
import dev.usbharu.tolo.communication.v1.ListThreadsRequest
import dev.usbharu.tolo.communication.v1.Message
import dev.usbharu.tolo.communication.v1.Point
import dev.usbharu.tolo.communication.v1.Staff
import dev.usbharu.tolo.communication.v1.StaffAssignment
import dev.usbharu.tolo.communication.v1.ThreadKind
import dev.usbharu.tolo.communication.v1.ThreadStatus
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.time.Instant

data class PagedResult<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
)

class GrpcOperationsPollingRemoteDataSource(
    private val grpcClient: GrpcCommunicationClient,
    private val session: AppSessionContext,
) : OperationsPollingRemoteDataSource {
    private val logger = AppLogger.withTag("GrpcOperationsRemoteDataSource")

    override suspend fun listPoints(): List<OperationPoint> = paginate { token ->
        val response = grpcClient.referenceService.ListPoints(
            ListPointsRequest {
                scope = session.toProtoScope()
                page = pageRequest(DEFAULT_PAGE_SIZE, token)
            }
        )
        PagedResult(response.points.map(Point::toOperationPoint), response.page.nextPageToken.ifBlank { null })
    }.also { logger.debug { "Fetched points via gRPC: count=${it.size}" } }

    override suspend fun listStaff(): List<OperationStaff> = paginate { token ->
        val response = grpcClient.referenceService.ListStaff(
            ListStaffRequest {
                scope = session.toProtoScope()
                page = pageRequest(DEFAULT_PAGE_SIZE, token)
            }
        )
        PagedResult(response.staff.map(Staff::toOperationStaff), response.page.nextPageToken.ifBlank { null })
    }.also { logger.debug { "Fetched staff via gRPC: count=${it.size}" } }

    override suspend fun listAssignments(): List<OperationAssignment> = paginate { token ->
        listAssignmentsPage(pageToken = token)
    }.also { logger.debug { "Fetched assignments via gRPC: count=${it.size}" } }

    override suspend fun listInstructions(): List<OperationInstruction> = paginate { token ->
        val response = grpcClient.instructionService.ListInstructions(
            ListInstructionsRequest {
                scope = session.toProtoScope()
                page = pageRequest(DEFAULT_PAGE_SIZE, token)
            }
        )
        PagedResult(
            response.instructions.map(Instruction::toOperationInstruction),
            response.page.nextPageToken.ifBlank { null },
        )
    }.also { logger.debug { "Fetched instructions via gRPC: count=${it.size}" } }

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> {
        val assignments = listAssignments()
        val assignedPointIds = assignments
            .filter { it.staffId == staffId && it.status in CURRENT_ASSIGNMENT_STATUSES }
            .mapTo(mutableSetOf()) { it.pointId }
        return listInstructions().filter { instruction ->
            instruction.targetType == OperationInstructionTargetType.ALL_EVENT_STAFF ||
                instruction.targetType == OperationInstructionTargetType.STAFF && instruction.targetId == staffId ||
                instruction.targetType == OperationInstructionTargetType.POINT && instruction.targetId in assignedPointIds
        }
    }

    override suspend fun listThreads(): List<OperationThread> = paginate { token ->
        listThreadsPage(pageToken = token)
    }.also { logger.debug { "Fetched threads via gRPC: count=${it.size}" } }

    override suspend fun listMessages(): List<OperationMessage> = listThreads()
        .flatMap { listThreadMessages(it.threadId) }
        .sortedOperationMessages()

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> = paginate { token ->
        listThreadMessagesPage(threadId = threadId, pageToken = token)
    }.also { logger.debug { "Fetched thread messages via gRPC: threadId=$threadId, count=${it.size}" } }

    suspend fun listAssignmentsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationAssignment> {
        val response = grpcClient.assignmentService.ListAssignments(
            ListAssignmentsRequest {
                scope = session.toProtoScope()
                page = pageRequest(pageSize, pageToken)
            }
        )
        return PagedResult(
            response.assignments.map(StaffAssignment::toOperationAssignment),
            response.page.nextPageToken.ifBlank { null },
        )
    }

    suspend fun listRelevantInstructionsPage(
        staffId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationInstruction> {
        if (pageToken.isNotBlank()) return PagedResult(emptyList())
        return PagedResult(listRelevantInstructions(staffId).take(pageSize))
    }

    suspend fun listThreadsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationThread> {
        val response = grpcClient.threadService.ListThreads(
            ListThreadsRequest {
                scope = session.toProtoScope()
                participantStaffId = session.staffId.toProtoStaffId()
                statuses += ThreadStatus.THREAD_STATUS_OPEN
                statuses += ThreadStatus.THREAD_STATUS_CLOSED
                page = pageRequest(pageSize, pageToken)
            }
        )
        return PagedResult(
            response.threads.map(ConversationThread::toOperationThread),
            response.page.nextPageToken.ifBlank { null },
        )
    }

    suspend fun listThreadMessagesPage(
        threadId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationMessage> {
        val response = grpcClient.messageService.ListMessages(
            ListMessagesRequest {
                scope = session.toProtoScope()
                this.threadId = threadId.toProtoThreadId()
                page = pageRequest(pageSize, pageToken)
            }
        )
        return PagedResult(
            response.messages.mapNotNull(Message::toOperationMessage),
            response.page.nextPageToken.ifBlank { null },
        )
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 100
        val CURRENT_ASSIGNMENT_STATUSES = setOf(
            OperationAssignmentStatus.ACTIVE,
            OperationAssignmentStatus.SCHEDULED,
        )
    }
}

internal fun Point.toOperationPoint(): OperationPoint = OperationPoint(
    updatedAt = "",
    reason = "grpc.list",
    entityId = id.value,
    pointId = id.value,
    name = displayName?.takeIf(String::isNotBlank) ?: id.value,
)

internal fun Staff.toOperationStaff(): OperationStaff = OperationStaff(
    updatedAt = syncedAt.toIsoStringOrEmpty(),
    reason = "grpc.list",
    entityId = id.value,
    staffId = id.value,
    name = displayName.ifBlank { id.value },
)

internal fun StaffAssignment.toOperationAssignment(): OperationAssignment = OperationAssignment(
    updatedAt = updatedAt.toIsoStringOrEmpty(),
    reason = "grpc.list",
    entityId = id.value,
    assignId = id.value,
    pointId = pointId.value,
    staffId = staffId.value,
    status = when (status) {
        AssignmentStatus.ASSIGNMENT_STATUS_ACTIVE -> OperationAssignmentStatus.ACTIVE
        AssignmentStatus.ASSIGNMENT_STATUS_ENDED -> OperationAssignmentStatus.ENDED
        AssignmentStatus.ASSIGNMENT_STATUS_CANCELLED -> OperationAssignmentStatus.CANCELLED
        AssignmentStatus.ASSIGNMENT_STATUS_SCHEDULED,
        AssignmentStatus.ASSIGNMENT_STATUS_UNSPECIFIED,
        is AssignmentStatus.UNRECOGNIZED,
        -> OperationAssignmentStatus.SCHEDULED
    },
    version = version,
)

internal fun Instruction.toOperationInstruction(): OperationInstruction {
    val (targetType, targetId) = when (val target = target.target) {
        is InstructionTarget.Target.AllEventStaff -> OperationInstructionTargetType.ALL_EVENT_STAFF to null
        is InstructionTarget.Target.StaffId -> OperationInstructionTargetType.STAFF to target.value.value
        is InstructionTarget.Target.PointId -> OperationInstructionTargetType.POINT to target.value.value
        null -> OperationInstructionTargetType.UNKNOWN to null
    }
    return OperationInstruction(
        updatedAt = updatedAt.toIsoStringOrEmpty(),
        reason = "grpc.list",
        entityId = id.value,
        instructionId = id.value,
        targetType = targetType,
        targetId = targetId,
        body = body,
        status = when (status) {
            InstructionStatus.INSTRUCTION_STATUS_COMPLETED -> OperationInstructionStatus.COMPLETED
            InstructionStatus.INSTRUCTION_STATUS_CANCELLED -> OperationInstructionStatus.CANCELLED
            InstructionStatus.INSTRUCTION_STATUS_EXPIRED -> OperationInstructionStatus.EXPIRED
            InstructionStatus.INSTRUCTION_STATUS_ISSUED,
            InstructionStatus.INSTRUCTION_STATUS_ACKNOWLEDGED,
            InstructionStatus.INSTRUCTION_STATUS_UNSPECIFIED,
            is InstructionStatus.UNRECOGNIZED,
            -> OperationInstructionStatus.ISSUED
        },
        version = version,
    )
}

internal fun ConversationThread.toOperationThread(): OperationThread = OperationThread(
    updatedAt = updatedAt.toIsoStringOrEmpty(),
    reason = "grpc.list",
    entityId = id.value,
    threadId = id.value,
    members = participantStaffIds.map { it.value },
    displayTitle = title,
    kind = when (kind) {
        ThreadKind.THREAD_KIND_GENERAL -> OperationThreadKind.GENERAL
        ThreadKind.THREAD_KIND_LOCATION -> OperationThreadKind.LOCATION
        ThreadKind.THREAD_KIND_DIRECT -> OperationThreadKind.DIRECT
        ThreadKind.THREAD_KIND_UNSPECIFIED,
        is ThreadKind.UNRECOGNIZED,
        -> OperationThreadKind.UNKNOWN
    },
    pointId = pointId.value.ifBlank { null },
    status = when (status) {
        ThreadStatus.THREAD_STATUS_CLOSED -> OperationThreadStatus.CLOSED
        ThreadStatus.THREAD_STATUS_ARCHIVED -> OperationThreadStatus.ARCHIVED
        ThreadStatus.THREAD_STATUS_OPEN,
        ThreadStatus.THREAD_STATUS_UNSPECIFIED,
        is ThreadStatus.UNRECOGNIZED,
        -> OperationThreadStatus.OPEN
    },
    version = version,
)

internal fun Message.toOperationMessage(): OperationMessage? {
    if (deletedAt.seconds != 0L || deletedAt.nanos != 0) return null
    return OperationMessage(
        updatedAt = createdAt.toIsoStringOrEmpty(),
        reason = "grpc.list",
        entityId = id.value,
        messageId = id.value,
        threadId = threadId.value,
        staffId = senderStaffId.value,
        text = body,
    )
}

internal fun Timestamp?.toIsoString(): String = this?.let {
    if (seconds == 0L && nanos == 0) "" else Instant.fromEpochSeconds(seconds, nanos).toString()
}.orEmpty()
