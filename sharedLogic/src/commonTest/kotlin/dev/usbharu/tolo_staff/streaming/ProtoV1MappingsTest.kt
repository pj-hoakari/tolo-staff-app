package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.v1.AllEventStaffTarget
import dev.usbharu.tolo.communication.v1.AssignmentId
import dev.usbharu.tolo.communication.v1.AssignmentStatus
import dev.usbharu.tolo.communication.v1.ConversationThread
import dev.usbharu.tolo.communication.v1.Instruction
import dev.usbharu.tolo.communication.v1.InstructionId
import dev.usbharu.tolo.communication.v1.InstructionStatus
import dev.usbharu.tolo.communication.v1.InstructionTarget
import dev.usbharu.tolo.communication.v1.Message
import dev.usbharu.tolo.communication.v1.MessageId
import dev.usbharu.tolo.communication.v1.Point
import dev.usbharu.tolo.communication.v1.PointId
import dev.usbharu.tolo.communication.v1.Staff
import dev.usbharu.tolo.communication.v1.StaffAssignment
import dev.usbharu.tolo.communication.v1.StaffId
import dev.usbharu.tolo.communication.v1.ThreadId
import dev.usbharu.tolo.communication.v1.ThreadKind
import dev.usbharu.tolo.communication.v1.ThreadStatus
import dev.usbharu.tolo.communication.v1.invoke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProtoV1MappingsTest {
    @Test
    fun `reference mappings fall back to identifiers when names are absent`() {
        val point = Point { id = PointId { value = "point-1" } }.toOperationPoint()
        val staff = Staff { id = StaffId { value = "staff-1" } }.toOperationStaff()

        assertEquals("point-1", point.name)
        assertEquals("staff-1", staff.name)
    }

    @Test
    fun `assignment unspecified and unknown statuses safely map to scheduled`() {
        fun assignment(status: AssignmentStatus) = StaffAssignment {
            id = AssignmentId { value = "assignment-1" }
            pointId = PointId { value = "point-1" }
            staffId = StaffId { value = "staff-1" }
            this.status = status
            version = 7
        }.toOperationAssignment()

        assertEquals(OperationAssignmentStatus.SCHEDULED, assignment(AssignmentStatus.ASSIGNMENT_STATUS_UNSPECIFIED).status)
        assertEquals(OperationAssignmentStatus.SCHEDULED, assignment(AssignmentStatus.UNRECOGNIZED(99)).status)
        assertEquals(OperationAssignmentStatus.ACTIVE, assignment(AssignmentStatus.ASSIGNMENT_STATUS_ACTIVE).status)
        assertEquals(7, assignment(AssignmentStatus.ASSIGNMENT_STATUS_ACTIVE).version)
    }

    @Test
    fun `instruction oneof targets and status labels map without inferred fields`() {
        fun instruction(target: InstructionTarget, status: InstructionStatus) = Instruction {
            id = InstructionId { value = "instruction-1" }
            this.target = target
            body = "先頭行\n詳細"
            this.status = status
        }.toOperationInstruction()

        val all = instruction(
            InstructionTarget { target = InstructionTarget.Target.AllEventStaff(AllEventStaffTarget {}) },
            InstructionStatus.INSTRUCTION_STATUS_ISSUED,
        )
        val point = instruction(
            InstructionTarget { target = InstructionTarget.Target.PointId(PointId { value = "point-1" }) },
            InstructionStatus.INSTRUCTION_STATUS_COMPLETED,
        )
        val absent = instruction(InstructionTarget {}, InstructionStatus.UNRECOGNIZED(99))

        assertEquals(OperationInstructionTargetType.ALL_EVENT_STAFF, all.targetType)
        assertEquals("先頭行", all.title)
        assertEquals(OperationInstructionTargetType.POINT, point.targetType)
        assertEquals("point-1", point.targetId)
        assertEquals(OperationInstructionStatus.COMPLETED, point.status)
        assertEquals(OperationInstructionTargetType.UNKNOWN, absent.targetType)
        assertEquals(OperationInstructionStatus.ISSUED, absent.status)
    }

    @Test
    fun `thread optional point and unknown enums have safe defaults`() {
        val thread = ConversationThread {
            id = ThreadId { value = "thread-1" }
            kind = ThreadKind.UNRECOGNIZED(99)
            status = ThreadStatus.THREAD_STATUS_UNSPECIFIED
            participantStaffIds += StaffId { value = "staff-1" }
        }.toOperationThread()

        assertEquals(OperationThreadKind.UNKNOWN, thread.kind)
        assertEquals(OperationThreadStatus.OPEN, thread.status)
        assertNull(thread.pointId)
        assertEquals(listOf("staff-1"), thread.members)
    }

    @Test
    fun `deleted messages are excluded`() {
        fun message(deleted: Boolean) = Message {
            id = MessageId { value = "message-1" }
            threadId = ThreadId { value = "thread-1" }
            senderStaffId = StaffId { value = "staff-1" }
            body = "本文"
            if (deleted) deletedAt = Timestamp { seconds = 1 }
        }

        assertEquals("本文", message(false).toOperationMessage()?.text)
        assertNull(message(true).toOperationMessage())
    }
}
