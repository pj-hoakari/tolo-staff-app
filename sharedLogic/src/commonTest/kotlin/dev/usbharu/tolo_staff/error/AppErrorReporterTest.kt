package dev.usbharu.tolo_staff.error

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.rpc.grpc.GrpcStatusCode
import kotlinx.rpc.grpc.asException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppErrorReporterTest {
    @Test
    fun `connection failure reported before subscription remains pending`() = runTest {
        val reporter = AppErrorReporter()

        reporter.reportConnectionFailure(
            AppErrorSource.GRPC,
            GrpcStatusCode.UNAVAILABLE.asException(),
            operation = "Load data",
            connectionKey = "data",
        )

        val event = reporter.events.first()
        assertEquals(AppErrorSource.GRPC, event.source)
        assertEquals(AppErrorPresentation.TRANSIENT, event.presentation)
        assertTrue(event.eventId > 0)
        reporter.acknowledge(event.eventId)
    }

    @Test
    fun `unacknowledged event survives collector replacement`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportConnectionFailure(AppErrorSource.GRPC, operation = "Load data", connectionKey = "data")

        val receivedByOldCollector = reporter.events.first()
        val receivedByNewCollector = reporter.events.first()

        assertEquals(receivedByOldCollector.eventId, receivedByNewCollector.eventId)
        reporter.acknowledge(receivedByNewCollector.eventId)
    }

    @Test
    fun `another operation recovery does not clear an active source failure`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportConnectionFailure(AppErrorSource.GRPC, operation = "Load staff", connectionKey = "staff")
        val initial = reporter.events.first()
        assertEquals(AppErrorSource.GRPC, initial.source)
        reporter.acknowledge(initial.eventId)

        reporter.reportConnectionFailure(AppErrorSource.GRPC, operation = "Load points", connectionKey = "points")
        reporter.reportConnectionRecovered(AppErrorSource.GRPC, "points")

        val unexpected = async(UnconfinedTestDispatcher(testScheduler)) { reporter.events.first() }
        reporter.reportConnectionFailure(AppErrorSource.GRPC, operation = "Load points", connectionKey = "points")
        runCurrent()
        assertFalse(unexpected.isCompleted)
        unexpected.cancel()

        reporter.reportConnectionRecovered(AppErrorSource.GRPC, "staff")
        reporter.reportConnectionRecovered(AppErrorSource.GRPC, "points")
        reporter.reportConnectionFailure(AppErrorSource.GRPC, operation = "Reload staff", connectionKey = "staff")

        val restored = reporter.events.first()
        assertEquals(AppErrorSource.GRPC, restored.source)
        reporter.acknowledge(restored.eventId)
    }

    @Test
    fun `fatal failure emits dialog without exposing exception text`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportFatal(IllegalStateException("private backend detail"), "Submit report")

        val event = reporter.events.first()
        assertEquals(AppErrorSource.APPLICATION, event.source)
        assertEquals(AppErrorPresentation.DIALOG, event.presentation)
        reporter.acknowledge(event.eventId)
    }

    @Test
    fun `backend failure selects presentation from failure type`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportBackendFailure(AppErrorSource.GRPC, GrpcStatusCode.UNAVAILABLE.asException(), "Load")
        val connection = reporter.events.first()
        assertEquals(AppErrorPresentation.TRANSIENT, connection.presentation)
        reporter.acknowledge(connection.eventId)

        reporter.reportBackendFailure(
            AppErrorSource.GRPC,
            GrpcStatusCode.INVALID_ARGUMENT.asException(),
            "Validate load request",
        )
        val fatal = reporter.events.first()
        assertEquals(AppErrorPresentation.DIALOG, fatal.presentation)
        reporter.acknowledge(fatal.eventId)
    }

    @Test
    fun `continuing fatal backend failure is not requeued after acknowledgement`() = runTest {
        val reporter = AppErrorReporter()
        val failure = GrpcStatusCode.INVALID_ARGUMENT.asException()
        reporter.reportBackendFailure(AppErrorSource.GRPC, failure, "Poll staff", "staff")
        reporter.acknowledge(reporter.events.first().eventId)

        val unexpected = async(UnconfinedTestDispatcher(testScheduler)) { reporter.events.first() }
        reporter.reportBackendFailure(AppErrorSource.GRPC, failure, "Poll staff", "staff")
        runCurrent()
        assertFalse(unexpected.isCompleted)
        unexpected.cancel()

        reporter.reportBackendRecovered(AppErrorSource.GRPC, "staff")
        reporter.reportBackendFailure(AppErrorSource.GRPC, failure, "Poll staff", "staff")
        val afterRecovery = reporter.events.first()
        assertEquals(AppErrorPresentation.DIALOG, afterRecovery.presentation)
        reporter.acknowledge(afterRecovery.eventId)
    }

    @Test
    fun `user action fatal failure can be reported again after acknowledgement`() = runTest {
        val reporter = AppErrorReporter()
        val failure = GrpcStatusCode.INVALID_ARGUMENT.asException()

        reporter.reportBackendFailure(
            source = AppErrorSource.GRPC,
            throwable = failure,
            operation = "Submit report",
            connectionKey = "report.submit",
            deduplicateFatalUntilRecovery = false,
        )
        val first = reporter.events.first()
        reporter.acknowledge(first.eventId)

        reporter.reportBackendFailure(
            source = AppErrorSource.GRPC,
            throwable = failure,
            operation = "Submit report",
            connectionKey = "report.submit",
            deduplicateFatalUntilRecovery = false,
        )
        val retried = reporter.events.first()

        assertTrue(retried.eventId > first.eventId)
        assertEquals(AppErrorPresentation.DIALOG, retried.presentation)
        reporter.acknowledge(retried.eventId)
    }

    @Test
    fun `coroutine cancellation is rethrown without emitting an error event`() = runTest {
        val reporter = AppErrorReporter()

        assertFailsWith<CancellationException> {
            reporter.reportBackendFailure(
                source = AppErrorSource.GRPC,
                throwable = CancellationException("view model cleared"),
                operation = "Submit report",
                connectionKey = "report.submit",
                deduplicateFatalUntilRecovery = false,
            )
        }

        val unexpected = async(UnconfinedTestDispatcher(testScheduler)) { reporter.events.first() }
        runCurrent()
        assertFalse(unexpected.isCompleted)
        unexpected.cancel()
    }

    @Test
    fun `different fatal failure keys with the same operation are both queued`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportFatal(IllegalStateException("first"), "Save", failureKey = "save:first")
        reporter.reportFatal(IllegalStateException("second"), "Save", failureKey = "save:second")

        val first = reporter.events.first()
        reporter.acknowledge(first.eventId)
        val second = reporter.events.first()

        assertTrue(second.eventId > first.eventId)
        reporter.acknowledge(second.eventId)
    }

    @Test
    fun `fatal rejected by a full queue can be reported after capacity is available`() = runTest {
        val reporter = AppErrorReporter()
        repeat(32) { index ->
            reporter.reportFatal(
                IllegalStateException("failure-$index"),
                operation = "Operation $index",
                failureKey = "failure:$index",
            )
        }
        reporter.reportFatal(
            IllegalStateException("overflow"),
            operation = "Overflow operation",
            failureKey = "failure:overflow",
        )

        val first = reporter.events.first()
        reporter.acknowledge(first.eventId)
        reporter.reportFatal(
            IllegalStateException("overflow"),
            operation = "Overflow operation",
            failureKey = "failure:overflow",
        )

        var lastEventId = first.eventId
        repeat(32) {
            val event = reporter.events.first()
            lastEventId = event.eventId
            reporter.acknowledge(event.eventId)
        }
        assertEquals(33L, lastEventId)
    }

    @Test
    fun `connection classifier recognizes transport failures only`() {
        assertTrue(GrpcStatusCode.UNAVAILABLE.asException().isConnectionFailure())
        assertTrue(GrpcStatusCode.DEADLINE_EXCEEDED.asException().isConnectionFailure())
        assertTrue(IllegalStateException("wrapper", GrpcStatusCode.UNAVAILABLE.asException()).isConnectionFailure())
        assertFalse(IllegalStateException("Status UNAVAILABLE").isConnectionFailure())
        assertFalse(IllegalStateException("Network configuration is invalid").isConnectionFailure())
        assertFalse(IllegalArgumentException("invalid report category").isConnectionFailure())
    }
}
