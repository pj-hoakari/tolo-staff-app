package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus
import tolo.operation.v1.ApplyReassignmentRequest
import tolo.operation.v1.invoke

class GrpcAssignmentStatusService(
    private val grpcClient: GrpcCommunicationClient,
    private val eventId: String = "",
    private val tenantId: String = "",
) : AssignmentStatusService {
    private val logger = AppLogger.withTag("GrpcAssignmentStatusService")

    init {
        if (eventId.isBlank() || tenantId.isBlank()) {
            logger.warn {
                "Operations scope is not configured: eventIdBlank=${eventId.isBlank()}, tenantIdBlank=${tenantId.isBlank()}"
            }
        }
    }

    override suspend fun updateStatus(assignId: String, status: OperationAssignmentStatus) {
        logger.trace { "updateStatus started: assignId=$assignId, status=$status" }
        if (status != OperationAssignmentStatus.ACTIVE) {
            logger.debug {
                "Skipping assignment status update because the operation API only supports applying an assignment: assignId=$assignId, status=$status"
            }
            return
        }
        grpcClient.operationControlService.ApplyReassignment(
            ApplyReassignmentRequest {
                reassignmentId = assignId
                this.eventId = this@GrpcAssignmentStatusService.eventId
                this.tenantId = this@GrpcAssignmentStatusService.tenantId
            }
        )
        logger.info { "updateStatus completed: assignId=$assignId, status=$status" }
    }
}
