package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FixedCurrentStaffSessionTest {
    @Test
    fun `session always keeps configured staff id and only resolves its display name`() = runTest {
        val dataSource = TestOperationsStreamDataSource(
            staff = listOf(OperationStaff(staffId = "staff-2", name = "別のスタッフ")),
        )
        val session = FixedCurrentStaffSession("staff-1", dataSource, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()

        assertEquals("staff-1", session.currentStaff.value.staffId)
        assertEquals("staff-1", session.currentStaff.value.displayName)
        assertTrue(session.isReady.value)

        dataSource.staffFlow.value = listOf(OperationStaff(staffId = "staff-1", name = "田中"))
        advanceUntilIdle()

        assertEquals(CurrentStaffMember("staff-1", "田中"), session.currentStaff.value)
        assertEquals(1, dataSource.startCount)
    }
}
