package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.AssignmentStatus
import dev.usbharu.tolo.communication.grpc.UpdateAssignmentStatusRequest
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus

class GrpcAssignmentStatusService(
    private val grpcClient: GrpcCommunicationClient,
) : AssignmentStatusService {
    private val logger = AppLogger.withTag("GrpcAssignmentStatusService")

    override suspend fun updateStatus(assignId: String, status: OperationAssignmentStatus) {
        logger.trace { "updateStatus started: assignId=$assignId, status=$status" }
        grpcClient.assignmentService.UpdateAssignmentStatus(
            UpdateAssignmentStatusRequest {
                this.assignId = assignId
                this.status = status.toGrpcStatus()
            }
        )
        logger.info { "updateStatus completed: assignId=$assignId, status=$status" }
    }
}

private fun OperationAssignmentStatus.toGrpcStatus(): AssignmentStatus = when (this) {
    OperationAssignmentStatus.ACTIVE -> AssignmentStatus.ASSIGNMENT_STATUS_ACTIVE
    OperationAssignmentStatus.EN_ROUTE -> AssignmentStatus.ASSIGNMENT_STATUS_EN_ROUTE
    OperationAssignmentStatus.PENDING -> AssignmentStatus.ASSIGNMENT_STATUS_PENDING
}
