package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationsOverviewRepositoryV1Test {
    @Test
    fun `active assignment takes priority and ended assignments are ignored`() = runTest {
        val dataSource = TestOperationsStreamDataSource(
            points = listOf(
                OperationPoint(pointId = "scheduled-point", name = "待機場所"),
                OperationPoint(pointId = "active-point", name = "現在地"),
                OperationPoint(pointId = "ended-point", name = "終了地点"),
            ),
            assignments = listOf(
                assignment("scheduled", "scheduled-point", OperationAssignmentStatus.SCHEDULED, 2),
                assignment("ended", "ended-point", OperationAssignmentStatus.ENDED, 4),
                assignment("active", "active-point", OperationAssignmentStatus.ACTIVE, 7),
            ),
        )

        val projection = OperationsOverviewRepositoryImpl(dataSource).observeOverview("staff-1").first()

        assertEquals("active", projection.currentAssignmentId)
        assertEquals(OperationAssignmentStatus.ACTIVE, projection.currentAssignmentStatus)
        assertEquals("現在地", projection.currentPlacementName)
        assertEquals(7, projection.currentAssignmentVersion)
    }

    private fun assignment(id: String, pointId: String, status: OperationAssignmentStatus, version: Long) =
        OperationAssignment(
            assignId = id,
            pointId = pointId,
            staffId = "staff-1",
            status = status,
            version = version,
        )
}
