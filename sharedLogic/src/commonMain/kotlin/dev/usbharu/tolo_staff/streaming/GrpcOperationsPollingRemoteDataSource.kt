package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Empty
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.Assignment
import dev.usbharu.tolo.communication.grpc.AssignmentRpc
import dev.usbharu.tolo.communication.grpc.AssignmentStatus
import dev.usbharu.tolo.communication.grpc.Instruction
import dev.usbharu.tolo.communication.grpc.InstructionRpc
import dev.usbharu.tolo.communication.grpc.InstructionStatus
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.Point
import dev.usbharu.tolo.communication.grpc.PointRpc
import dev.usbharu.tolo.communication.grpc.Staff
import dev.usbharu.tolo.communication.grpc.StaffIdRequest
import dev.usbharu.tolo.communication.grpc.StaffRpc
import dev.usbharu.tolo.communication.grpc.Thread
import dev.usbharu.tolo.communication.grpc.ThreadIdRequest
import dev.usbharu.tolo.communication.grpc.ThreadRpc
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger

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
        grpcClient.assignmentService.ListAssignments(Empty {}).assignments
            .map { it.toOperationAssignment() }
            .also { logger.debug { "Fetched assignments via gRPC: count=${it.size}" } }

    override suspend fun listInstructions(): List<OperationInstruction> =
        grpcClient.instructionService.ListInstructions(Empty {}).instructions
            .map { it.toOperationInstruction() }
            .also { logger.debug { "Fetched instructions via gRPC: count=${it.size}" } }

    override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> =
        grpcClient.instructionService.ListRelevantInstructions(
            StaffIdRequest {
                this.staffId = staffId
            }
        ).instructions
            .map { it.toOperationInstruction() }
            .also {
                logger.debug {
                    "Fetched relevant instructions via gRPC: staffId=$staffId, count=${it.size}"
                }
            }

    override suspend fun listThreads(): List<OperationThread> =
        grpcClient.threadService.ListThreads(Empty {}).threads
            .map { it.toOperationThread() }
            .also { logger.debug { "Fetched threads via gRPC: count=${it.size}" } }

    override suspend fun listMessages(): List<OperationMessage> =
        grpcClient.messageService.ListMessages(Empty {}).messages
            .map { it.toOperationMessage() }
            .also { logger.debug { "Fetched messages via gRPC: count=${it.size}" } }

    override suspend fun listThreadMessages(threadId: String): List<OperationMessage> =
        grpcClient.threadService.ListThreadMessages(
            ThreadIdRequest {
                this.threadId = threadId
            }
        ).messages
            .map { it.toOperationMessage() }
            .also { logger.debug { "Fetched thread messages via gRPC: threadId=$threadId, count=${it.size}" } }
}

private fun Point.toOperationPoint(): OperationPoint = OperationPoint(
    updatedAt = "",
    reason = "grpc.list",
    entityId = pointId,
    pointId = pointId,
    name = name,
    description = description,
)

private fun Staff.toOperationStaff(): OperationStaff = OperationStaff(
    updatedAt = "",
    reason = "grpc.list",
    entityId = staffId,
    staffId = staffId,
    name = name,
    roles = roles,
)

private fun Assignment.toOperationAssignment(): OperationAssignment = OperationAssignment(
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

private fun Instruction.toOperationInstruction(): OperationInstruction = OperationInstruction(
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
)

private fun Thread.toOperationThread(): OperationThread = OperationThread(
    updatedAt = "",
    reason = "grpc.list",
    entityId = threadId,
    threadId = threadId,
    members = memberStaffIds,
)

private fun Message.toOperationMessage(): OperationMessage {
    val payload = payload
    return when (payload) {
        is Message.Payload.Assign -> OperationMessage(
            updatedAt = "",
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.ASSIGN,
        )

        is Message.Payload.Unassign -> OperationMessage(
            updatedAt = "",
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.UNASSIGN,
        )

        is Message.Payload.Instruction -> OperationMessage(
            updatedAt = "",
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.INSTRUCTION,
            instructionId = payload.value.instructionId,
        )

        is Message.Payload.Report -> OperationMessage(
            updatedAt = "",
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.REPORT,
            reportId = payload.value.reportId,
        )

        is Message.Payload.Simple -> OperationMessage(
            updatedAt = "",
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
            updatedAt = "",
            reason = "grpc.list",
            entityId = messageId,
            messageId = messageId,
            threadId = threadId,
            staffId = staffId,
            messageType = OperationMessageType.SIMPLE,
        )
    }
}
