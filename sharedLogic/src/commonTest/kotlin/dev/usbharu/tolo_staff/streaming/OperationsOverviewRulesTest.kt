package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.appshell.assignedPointIds
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationsOverviewRulesTest {
    @Test
    fun `instructions combine all staff current staff and current point targets`() {
        val assignments = listOf(
            assignment("staff-1", "active", OperationAssignmentStatus.ACTIVE),
            assignment("staff-1", "ended", OperationAssignmentStatus.ENDED),
            assignment("staff-2", "other", OperationAssignmentStatus.ACTIVE),
        )
        val instructions = listOf(
            instruction("all", OperationInstructionTargetType.ALL_EVENT_STAFF, null),
            instruction("me", OperationInstructionTargetType.STAFF, "staff-1"),
            instruction("active", OperationInstructionTargetType.POINT, "active"),
            instruction("ended", OperationInstructionTargetType.POINT, "ended"),
            instruction("other", OperationInstructionTargetType.STAFF, "staff-2"),
        )

        assertEquals(listOf("all", "me", "active"), instructions.relevantTo("staff-1", assignments).map { it.instructionId })
    }

    @Test
    fun `assigned points include active and scheduled only`() {
        val assignments = listOf(
            assignment("staff-1", "active", OperationAssignmentStatus.ACTIVE),
            assignment("staff-1", "scheduled", OperationAssignmentStatus.SCHEDULED),
            assignment("staff-1", "ended", OperationAssignmentStatus.ENDED),
            assignment("staff-1", "cancelled", OperationAssignmentStatus.CANCELLED),
        )

        assertEquals(setOf("active", "scheduled"), assignments.assignedPointIds("staff-1"))
    }

    private fun assignment(staffId: String, pointId: String, status: OperationAssignmentStatus) = OperationAssignment(
        assignId = "$staffId-$pointId",
        pointId = pointId,
        staffId = staffId,
        status = status,
    )

    private fun instruction(id: String, type: OperationInstructionTargetType, targetId: String?) = OperationInstruction(
        instructionId = id,
        targetType = type,
        targetId = targetId,
        body = id,
        status = OperationInstructionStatus.ISSUED,
    )
}
