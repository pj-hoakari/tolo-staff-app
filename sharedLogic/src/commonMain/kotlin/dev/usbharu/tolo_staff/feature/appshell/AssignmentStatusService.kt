package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus

interface AssignmentStatusService {
    suspend fun updateStatus(assignId: String, status: OperationAssignmentStatus)
}

class NoOpAssignmentStatusService : AssignmentStatusService {
    override suspend fun updateStatus(assignId: String, status: OperationAssignmentStatus) = Unit
}
