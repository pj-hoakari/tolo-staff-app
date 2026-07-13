package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionFailureResilienceTest {
    @Test
    fun `grpc backed stream keeps connection failures inside its refresh boundary`() = runTest {
        val uncaught = mutableListOf<Throwable>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val exceptionHandler = CoroutineExceptionHandler { _, throwable -> uncaught += throwable }
        val dataSource = GrpcBackedOperationsStreamDataSource(
            remoteDataSource = FailingOperationsRemoteDataSource(),
            coroutineContext = dispatcher + exceptionHandler,
        )

        dataSource.observePoints()
        dataSource.observeMessages("staff-1")
        advanceUntilIdle()

        assertTrue(uncaught.isEmpty())
        dataSource.stop()
    }

    @Test
    fun `polling emits an empty snapshot then retries after a connection failure`() = runTest {
        var attempts = 0
        val snapshots = SharedPollingFlowFactory(intervalMillis = 1).create {
            attempts += 1
            if (attempts == 1) {
                error("connection refused")
            }
            listOf("connected")
        }.take(2).toList()

        assertEquals(listOf(emptyList(), listOf("connected")), snapshots)
        assertEquals(2, attempts)
    }

    private class FailingOperationsRemoteDataSource : OperationsPollingRemoteDataSource {
        override suspend fun listPoints(): List<OperationPoint> = connectionRefused()
        override suspend fun listStaff(): List<OperationStaff> = connectionRefused()
        override suspend fun listAssignments(): List<OperationAssignment> = connectionRefused()
        override suspend fun listInstructions(): List<OperationInstruction> = connectionRefused()
        override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> = connectionRefused()
        override suspend fun listThreads(): List<OperationThread> = connectionRefused()
        override suspend fun listMessages(): List<OperationMessage> = connectionRefused()
        override suspend fun listThreadMessages(threadId: String): List<OperationMessage> = connectionRefused()

        private fun connectionRefused(): Nothing = error("connection refused")
    }
}
