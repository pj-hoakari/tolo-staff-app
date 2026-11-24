package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import tolo.kernel.v1.GraphAnchor
import tolo.kernel.v1.invoke
import tolo.operation.v1.Audience
import tolo.operation.v1.Reassignment
import tolo.operation.v1.ReassignmentStatus
import tolo.operation.v1.StaffMessage
import tolo.operation.v1.invoke

class GrpcOperationsPollingRemoteDataSourceTest {
    @Test
    fun `reassignment maps directed status to pending assignment`() {
        val reassignment = Reassignment {
            reassignmentId = "reassignment-1"
            eventId = "event-1"
            staffUserId = "tanaka"
            status = ReassignmentStatus.REASSIGNMENT_STATUS_DIRECTED
            target = Reassignment.Target.ObservationPointId("point-1")
        }

        val mapped = reassignment.toOperationAssignment()

        assertEquals("reassignment-1", mapped.assignId)
        assertEquals("tanaka", mapped.staffId)
        assertEquals("point-1", mapped.pointId)
        assertEquals(OperationAssignmentStatus.PENDING, mapped.status)
    }

    @Test
    fun `reassignment maps applied status to active assignment`() {
        val reassignment = Reassignment {
            reassignmentId = "reassignment-2"
            staffUserId = "tanaka"
            status = ReassignmentStatus.REASSIGNMENT_STATUS_APPLIED
            target = Reassignment.Target.GatePointId("gate-1")
        }

        val mapped = reassignment.toOperationAssignment()

        assertEquals("gate-1", mapped.pointId)
        assertEquals(OperationAssignmentStatus.ACTIVE, mapped.status)
    }

    @Test
    fun `reassignment anchor on a point maps to the anchor point id`() {
        val reassignment = Reassignment {
            reassignmentId = "reassignment-3"
            staffUserId = "tanaka"
            status = ReassignmentStatus.REASSIGNMENT_STATUS_DIRECTED
            target = Reassignment.Target.Anchor(
                GraphAnchor {
                    target = GraphAnchor.Target.PointId("point-2")
                }
            )
        }

        assertEquals("point-2", reassignment.toOperationAssignment().pointId)
    }

    @Test
    fun `reassignment anchor on a route leaves the point id empty`() {
        val reassignment = Reassignment {
            reassignmentId = "reassignment-4"
            staffUserId = "tanaka"
            status = ReassignmentStatus.REASSIGNMENT_STATUS_DIRECTED
            target = Reassignment.Target.Anchor(
                GraphAnchor {
                    target = GraphAnchor.Target.RouteId("route-1")
                }
            )
        }

        assertEquals("", reassignment.toOperationAssignment().pointId)
    }

    @Test
    fun `staff message maps to active instruction for the requesting staff`() {
        val message = StaffMessage {
            messageId = "message-1"
            eventId = "event-1"
            senderUserId = "headquarters"
            audience = Audience.AUDIENCE_STAFF
            body = "列整理を開始\nAゲート前に誘導してください"
            sentAt = Timestamp {
                seconds = 1_750_324_800L
                nanos = 123_000_000
            }
        }

        val mapped = message.toOperationInstruction(staffIds = listOf("tanaka"))

        assertEquals("message-1", mapped.instructionId)
        assertEquals(listOf("tanaka"), mapped.staffIds)
        assertEquals("列整理を開始", mapped.title)
        assertEquals("Aゲート前に誘導してください", mapped.description)
        assertEquals(OperationInstructionStatus.ACTIVE, mapped.status)
        assertEquals(Instant.fromEpochSeconds(1_750_324_800L, 123_000_000).toString(), mapped.updatedAt)
    }

    @Test
    fun `single line staff message keeps full body as description`() {
        val message = StaffMessage {
            messageId = "message-2"
            senderUserId = "headquarters"
            audience = Audience.AUDIENCE_STAFF
            body = "配置を確認してください"
            sentAt = Timestamp { seconds = 1_750_324_800L }
        }

        val mapped = message.toOperationInstruction(staffIds = listOf("tanaka"))

        assertEquals("本部からの連絡", mapped.title)
        assertEquals("配置を確認してください", mapped.description)
    }
}
