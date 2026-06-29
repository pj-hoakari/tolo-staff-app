package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Empty
import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.Assignment
import dev.usbharu.tolo.communication.grpc.AssignmentRpc
import dev.usbharu.tolo.communication.grpc.AssignmentStatus
import dev.usbharu.tolo.communication.grpc.Instruction
import dev.usbharu.tolo.communication.grpc.InstructionRpc
import dev.usbharu.tolo.communication.grpc.InstructionStatus
import dev.usbharu.tolo.communication.grpc.ListAssignmentsRequest
import dev.usbharu.tolo.communication.grpc.ListMessagesRequest
import dev.usbharu.tolo.communication.grpc.ListRelevantInstructionsRequest
import dev.usbharu.tolo.communication.grpc.ListThreadsRequest
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.PageRequest
import dev.usbharu.tolo.communication.grpc.Point
import dev.usbharu.tolo.communication.grpc.PointRpc
import dev.usbharu.tolo.communication.grpc.Staff
import dev.usbharu.tolo.communication.grpc.StaffRpc
import dev.usbharu.tolo.communication.grpc.Thread
import dev.usbharu.tolo.communication.grpc.ThreadMessagesRequest
import dev.usbharu.tolo.communication.grpc.ThreadRpc
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlin.time.Instant

data class PagedResult<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
)

class GrpcOperationsPollingRemoteDataSource(
    private val grpcClient: GrpcCommunicationClient,
) : OperationsPollingRemoteDataSource {
    private val logger = AppLogger.withTag("GrpcOperationsPollingRemoteDataSource")

    override suspend fun listPoints(): List<OperationPoint> =
        grpcClient.pointService.ListPoints(Empty {}).points
            .map { it.toOperationPoint() }
            .also { logger.debug { "Fetched points via gRPC: count=${it.size}" } }

    override suspend fun listStaff(): List<OperationStaff> =
        grpcClient.staffService.ListStaff(Empty {}).staff
            .map { it.toOperationStaff() }
            .also { logger.debug { "Fetched staff via gRPC: count=${it.size}" } }

    override suspend fun listAssignments(): List<OperationAssignment> =
        paginate { pageToken ->
            listAssignmentsPage(pageToken = pageToken)
        }
            .also { logger.debug { "Fetched assignments via gRPC: count=${it.size}" } }

    override suspend fun listInstructions(): List<OperationInstruction> =
        grpcClient.instructionService.ListInstructions(Empty {}).instructions
            .map { it.toOperationInstruction() }
            .also { logger.debug { "Fetched instructions via gRPC: count=${it.size}" } }

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> =
        paginate { pageToken ->
            listRelevantInstructionsPage(staffId = staffId, pageToken = pageToken)
        }
            .also {
                logger.debug {
                    "Fetched relevant instructions via gRPC: staffId=$staffId, count=${it.size}"
                }
            }

    override suspend fun listThreads(): List<OperationThread> =
        paginate { pageToken ->
            listThreadsPage(pageToken = pageToken)
        }
            .also { logger.debug { "Fetched threads via gRPC: count=${it.size}" } }

    override suspend fun listMessages(): List<OperationMessage> =
        paginate { pageToken ->
            listMessagesPage(pageToken = pageToken)
        }
            .also { logger.debug { "Fetched messages via gRPC: count=${it.size}" } }

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> =
        paginate { pageToken ->
            listThreadMessagesPage(threadId = threadId, pageToken = pageToken)
        }.also { logger.debug { "Fetched thread messages via gRPC: threadId=$threadId, count=${it.size}" } }

    suspend fun listAssignmentsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationAssignment> =
        grpcClient.assignmentService.ListAssignments(
            ListAssignmentsRequest {
                page = pageRequest(pageSize, pageToken)
            }
        ).let { response ->
            PagedResult(
                items = response.assignments.map { it.toOperationAssignment() },
                nextPageToken = response.nextPageToken.takeIf(String::isNotBlank),
            )
        }

    suspend fun listRelevantInstructionsPage(
        staffId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationInstruction> =
        grpcClient.instructionService.ListRelevantInstructions(
            ListRelevantInstructionsRequest {
                this.staffId = staffId
                page = pageRequest(pageSize, pageToken)
            }
        ).let { response ->
            PagedResult(
                items = response.instructions.map { it.toOperationInstruction() },
                nextPageToken = response.nextPageToken.takeIf(String::isNotBlank),
            )
        }

    suspend fun listThreadsPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationThread> =
        grpcClient.threadService.ListThreads(
            ListThreadsRequest {
                page = pageRequest(pageSize, pageToken)
            }
        ).let { response ->
            PagedResult(
                items = response.threads.map { it.toOperationThread() },
                nextPageToken = response.nextPageToken.takeIf(String::isNotBlank),
            )
        }

    suspend fun listMessagesPage(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationMessage> =
        grpcClient.messageService.ListMessages(
            ListMessagesRequest {
                page = pageRequest(pageSize, pageToken)
            }
        ).let { response ->
            PagedResult(
                items = response.messages.map { it.toOperationMessage() },
                nextPageToken = response.nextPageToken.takeIf(String::isNotBlank),
            )
        }

    suspend fun listThreadMessagesPage(
        threadId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        pageToken: String = "",
    ): PagedResult<OperationMessage> =
        grpcClient.threadService.ListThreadMessages(
            ThreadMessagesRequest {
                this.threadId = threadId
                page = pageRequest(pageSize, pageToken)
            }
        ).let { response ->
            PagedResult(
                items = response.messages.map { it.toOperationMessage() },
                nextPageToken = response.nextPageToken.takeIf(String::isNotBlank),
            )
        }

    private suspend fun <T> paginate(
        fetchPage: suspend (pageToken: String) -> PagedResult<T>,
    ): List<T> {
        val items = mutableListOf<T>()
        var nextPageToken = ""
        do {
            val page = fetchPage(nextPageToken)
            items += page.items
            nextPageToken = page.nextPageToken.orEmpty()
        } while (nextPageToken.isNotBlank())
        return items
    }

    private fun pageRequest(pageSize: Int, pageToken: String): PageRequest = PageRequest {
        this.pageSize = pageSize
        this.pageToken = pageToken
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 100
    }
}

internal fun Point.toOperationPoint(): OperationPoint = OperationPoint(
    updatedAt = "",
    reason = "grpc.list",
    entityId = pointId,
    pointId = pointId,
    name = name,
    description = description,
)

internal fun Staff.toOperationStaff(): OperationStaff = OperationStaff(
    updatedAt = "",
    reason = "grpc.list",
    entityId = staffId,
    staffId = staffId,
    name = name,
    roles = roles,
)

internal fun Assignment.toOperationAssignment(): OperationAssignment = OperationAssignment(
    updatedAt = "",
    reason = "grpc.list",
    entityId = assignId,
    assignId = assignId,
    pointId = pointId,
    staffId = staffId,
    status = when (status) {
        AssignmentStatus.ASSIGNMENT_STATUS_ACTIVE -> OperationAssignmentStatus.ACTIVE
        AssignmentStatus.ASSIGNMENT_STATUS_EN_ROUTE -> OperationAssignmentStatus.EN_ROUTE
        AssignmentStatus.ASSIGNMENT_STATUS_PENDING -> OperationAssignmentStatus.PENDING
        is AssignmentStatus.UNRECOGNIZED -> OperationAssignmentStatus.PENDING
        AssignmentStatus.ASSIGNMENT_STATUS_UNSPECIFIED -> OperationAssignmentStatus.PENDING
    },
)

internal fun Instruction.toOperationInstruction(): OperationInstruction = OperationInstruction(
    updatedAt = "",
    reason = "grpc.list",
    entityId = instructionId,
    instructionId = instructionId,
    pointIds = pointIds,
    staffIds = staffIds,
    title = title,
    description = description,
    status = when (status) {
        InstructionStatus.INSTRUCTION_STATUS_ACTIVE -> OperationInstructionStatus.ACTIVE
        is InstructionStatus.UNRECOGNIZED -> OperationInstructionStatus.ACTIVE
        InstructionStatus.INSTRUCTION_STATUS_UNSPECIFIED -> OperationInstructionStatus.ACTIVE
    },
    threadId = threadId.ifBlank { null },
)

internal fun Thread.toOperationThread(): OperationThread = OperationThread(
    updatedAt = "",
    reason = "grpc.list",
    entityId = threadId,
    threadId = threadId,
    members = memberStaffIds,
    displayTitle = displayTitle,
)

internal fun Message.toOperationMessage(): OperationMessage {
    val payload = payload
    return when (payload) {
        is Message.Payload.Assign -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.ASSIGN,
        )

        is Message.Payload.Unassign -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.UNASSIGN,
        )

        is Message.Payload.Instruction -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.INSTRUCTION,
            instructionId = payload.value.instructionId,
        )

        is Message.Payload.Report -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.REPORT,
            reportId = payload.value.reportId,
        )

        is Message.Payload.Simple -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.SIMPLE,
            text = payload.value.text,
            replyTo = payload.value.replyTo,
        )

        else -> OperationMessage(
            updatedAt = createdAt.toIsoString(),
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.SIMPLE,
        )
    }
}

internal fun Timestamp?.toIsoString(): String = this
    ?.let { Instant.fromEpochSeconds(it.seconds, it.nanos).toString() }
    .orEmpty()
