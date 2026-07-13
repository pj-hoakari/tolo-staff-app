package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo.communication.v1.ActivateAssignmentRequest
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.toProtoAssignmentId
import dev.usbharu.tolo_staff.streaming.toProtoScope

class GrpcAssignmentStatusService(
    private val grpcClient: GrpcCommunicationClient,
    private val session: AppSessionContext,
) : AssignmentStatusService {
    private val logger = AppLogger.withTag("GrpcAssignmentStatusService")

    override suspend fun activate(assignmentId: String, expectedVersion: Long) {
        grpcClient.assignmentService.ActivateAssignment(
            activateAssignmentRequest(session, assignmentId, expectedVersion)
        )
        logger.info { "Activated assignment: assignmentId=$assignmentId, expectedVersion=$expectedVersion" }
    }
}

internal fun activateAssignmentRequest(
    session: AppSessionContext,
    assignmentId: String,
    expectedVersion: Long,
): ActivateAssignmentRequest = ActivateAssignmentRequest {
    scope = session.toProtoScope()
    this.assignmentId = assignmentId.toProtoAssignmentId()
    this.expectedVersion = expectedVersion
}
