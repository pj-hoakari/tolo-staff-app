package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus

interface AssignmentStatusService {
    suspend fun activate(assignmentId: String, expectedVersion: Long)
}

class NoOpAssignmentStatusService : AssignmentStatusService {
    override suspend fun activate(assignmentId: String, expectedVersion: Long) = Unit
}
