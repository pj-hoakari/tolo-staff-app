package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentStaffSessionTest {
    @Test
    fun `default selected staff is tanaka`() = runTest {
        val session = MockCurrentStaffSession(
            initialStaff = testStaffMembers(),
            coroutineContext = StandardTestDispatcher(testScheduler)
        )

        assertEquals(true, session.isReady.value)
        assertEquals("tanaka", session.currentStaffSnapshot.staffId)
        assertEquals("田中", session.currentStaffSnapshot.displayName)
    }

    @Test
    fun `empty initial session does not fall back to mock staff`() = runTest {
        val session = MockCurrentStaffSession(
            coroutineContext = StandardTestDispatcher(testScheduler)
        )

        assertEquals(true, session.isReady.value)
        assertEquals("unknown", session.currentStaffSnapshot.staffId)
        assertEquals("未取得", session.currentStaffSnapshot.displayName)
        assertEquals(emptyList(), session.availableStaff.value)
    }

    @Test
    fun `selecting known staff updates snapshot and stream`() = runTest {
        val session = MockCurrentStaffSession(
            initialStaff = testStaffMembers(),
            coroutineContext = StandardTestDispatcher(testScheduler)
        )

        session.selectStaff("sato")

        assertEquals("sato", session.currentStaffSnapshot.staffId)
        assertEquals("佐藤", session.currentStaffSnapshot.displayName)
    }

    @Test
    fun `streamed staff replaces available list while keeping matching selection`() = runTest {
        val dataSource = SessionFakeOperationsStreamDataSource()
        val session = MockCurrentStaffSession(
            dataSource = dataSource,
            initialStaff = testStaffMembers(),
            coroutineContext = StandardTestDispatcher(testScheduler)
        )

        dataSource.staffFlow.value = listOf(
            OperationStaff(
                updatedAt = "2026-06-18T09:00:00Z",
                reason = "seed",
                entityId = "staff-sato",
                staffId = "sato",
                name = "佐藤",
                roles = listOf("巡回担当"),
            )
        )
        advanceUntilIdle()

        assertEquals(true, session.isReady.value)
        assertEquals(listOf("sato"), session.availableStaff.value.map { it.staffId })
        assertEquals("sato", session.currentStaffSnapshot.staffId)
    }

    @Test
    fun `staff stream failure resolves session without fallback data`() = runTest {
        val session = MockCurrentStaffSession(
            dataSource = FailingSessionFakeOperationsStreamDataSource(),
            coroutineContext = StandardTestDispatcher(testScheduler)
        )

        advanceUntilIdle()

        assertEquals(true, session.isReady.value)
        assertEquals("unknown", session.currentStaffSnapshot.staffId)
        assertEquals(emptyList(), session.availableStaff.value)
    }
}

private class SessionFakeOperationsStreamDataSource : OperationsStreamDataSource {
    val staffFlow = MutableStateFlow(emptyList<OperationStaff>())

    override fun observePoints(): Flow<List<OperationPoint>> = flowOf(emptyList())

    override fun observeStaff(): Flow<List<OperationStaff>> = staffFlow

    override fun observeAssignments(): Flow<List<OperationAssignment>> = flowOf(emptyList())

    override fun observeInstructions(): Flow<List<OperationInstruction>> = flowOf(emptyList())

    override fun observeThreads(): Flow<List<OperationThread>> = flowOf(emptyList())

    override fun observeMessages(): Flow<List<OperationMessage>> = flowOf(emptyList())

    override fun start() = Unit

    override fun stop() = Unit
}

private class FailingSessionFakeOperationsStreamDataSource : OperationsStreamDataSource {
    override fun observePoints(): Flow<List<OperationPoint>> = flowOf(emptyList())

    override fun observeStaff(): Flow<List<OperationStaff>> = flow {
        throw IllegalStateException("staff unavailable")
    }

    override fun observeAssignments(): Flow<List<OperationAssignment>> = flowOf(emptyList())

    override fun observeInstructions(): Flow<List<OperationInstruction>> = flowOf(emptyList())

    override fun observeThreads(): Flow<List<OperationThread>> = flowOf(emptyList())

    override fun observeMessages(): Flow<List<OperationMessage>> = flowOf(emptyList())

    override fun start() = Unit

    override fun stop() = Unit
}

private fun testStaffMembers(): List<CurrentStaffMember> = listOf(
    CurrentStaffMember("tanaka", "田中", "Aゲート担当"),
    CurrentStaffMember("sato", "佐藤", "巡回担当"),
    CurrentStaffMember("yamada", "山田", "サブリーダー"),
)
