package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        val snapshots = SharedPollingFlowFactory(intervalMillis = 1).create("test.polling") {
            attempts += 1
            if (attempts == 1) {
                error("connection refused")
            }
            listOf("connected")
        }.take(2).toList()

        assertEquals(listOf(emptyList(), listOf("connected")), snapshots)
        assertEquals(2, attempts)
    }

    @Test
    fun `firestore session resync request refreshes grpc snapshots`() = runTest {
        val remote = CountingOperationsRemoteDataSource()
        val notifier = ResyncOperationsChangeNotifier()
        val dataSource = GrpcBackedOperationsStreamDataSource(
            remoteDataSource = remote,
            changeNotifier = notifier,
            coroutineContext = StandardTestDispatcher(testScheduler),
        )

        dataSource.observePoints()
        runCurrent()
        assertEquals(1, remote.pointsRequests)

        notifier.resync.emit(Unit)
        runCurrent()

        assertEquals(2, remote.pointsRequests)
        dataSource.stop()
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

    private class ResyncOperationsChangeNotifier : OperationsChangeNotifier {
        val resync = MutableSharedFlow<Unit>()
        override fun observeResyncRequests(): Flow<Unit> = resync
    }

    private class CountingOperationsRemoteDataSource : OperationsPollingRemoteDataSource {
        var pointsRequests = 0

        override suspend fun listPoints(): List<OperationPoint> {
            pointsRequests += 1
            return emptyList()
        }

        override suspend fun listStaff(): List<OperationStaff> = emptyList()
        override suspend fun listAssignments(): List<OperationAssignment> = emptyList()
        override suspend fun listInstructions(): List<OperationInstruction> = emptyList()
        override suspend fun listRelevantInstructions(staffId: String): List<OperationInstruction> = emptyList()
        override suspend fun listThreads(): List<OperationThread> = emptyList()
        override suspend fun listMessages(): List<OperationMessage> = emptyList()
        override suspend fun listThreadMessages(threadId: String): List<OperationMessage> = emptyList()
    }
}
