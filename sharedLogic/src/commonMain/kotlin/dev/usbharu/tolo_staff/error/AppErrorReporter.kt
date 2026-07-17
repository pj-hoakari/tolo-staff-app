package dev.usbharu.tolo_staff.error

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.logging.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.rpc.grpc.GrpcStatusCode
import kotlinx.rpc.grpc.GrpcStatusException
import kotlinx.rpc.grpc.status
import kotlinx.rpc.grpc.statusCode

enum class AppErrorSource {
    GRPC,
    FIREBASE,
    APPLICATION,
}

enum class AppErrorPresentation {
    TRANSIENT,
    DIALOG,
}

data class AppErrorEvent(
    val source: AppErrorSource,
    val presentation: AppErrorPresentation,
    val eventId: Long = 0,
)

data class ConnectionFailureKey(
    val source: AppErrorSource,
    val connectionKey: String,
)

class AppErrorReporter {
    private val logger = AppLogger.withTag("AppErrorReporter")
    private val activeConnectionFailures = MutableStateFlow(emptySet<ConnectionFailureKey>())
    private val activeFatalFailures = MutableStateFlow(emptySet<String>())
    private val pendingEvents = MutableStateFlow(emptyList<PendingEvent>())
    private val nextEventId = MutableStateFlow(1L)

    /**
     * Emits the oldest pending event and keeps it available across collector/lifecycle replacement
     * until [acknowledge] is called by the UI that accepted it for presentation.
     */
    val events: Flow<AppErrorEvent> = pendingEvents
        .map { it.firstOrNull()?.event }
        .filterNotNull()
        .distinctUntilChangedBy(AppErrorEvent::eventId)

    fun acknowledge(eventId: Long) {
        while (true) {
            val current = pendingEvents.value
            if (current.none { it.event.eventId == eventId }) return
            val updated = current.filterNot { it.event.eventId == eventId }
            if (pendingEvents.compareAndSet(current, updated)) return
        }
    }

    fun reportConnectionFailure(
        source: AppErrorSource,
        throwable: Throwable? = null,
        operation: String,
        connectionKey: String = operation,
    ) {
        require(source != AppErrorSource.APPLICATION) { "Connection failures require a connection source" }
        require(connectionKey.isNotBlank()) { "Connection key must not be blank" }
        val activation = activate(ConnectionFailureKey(source, connectionKey))
        if (activation.keyAdded) {
            if (throwable == null) {
                logger.warn { "$operation failed: source=$source, connectionKey=$connectionKey" }
            } else {
                logger.warn(throwable) { "$operation failed: source=$source, connectionKey=$connectionKey" }
            }
            if (activation.sourceBecameUnavailable) {
                enqueue(
                    event = AppErrorEvent(source, AppErrorPresentation.TRANSIENT),
                    deduplicationKey = "connection:$source",
                )
            }
        } else {
            logger.debug { "$operation is still unavailable: source=$source, connectionKey=$connectionKey" }
        }
    }

    fun reportConnectionRecovered(source: AppErrorSource, connectionKey: String) {
        val recovery = deactivate(ConnectionFailureKey(source, connectionKey))
        if (recovery.keyRemoved && recovery.sourceBecameAvailable) {
            logger.info { "Connection recovered: source=$source" }
        } else if (recovery.keyRemoved) {
            logger.debug { "Connection operation recovered: source=$source, connectionKey=$connectionKey" }
        }
    }

    fun reportBackendRecovered(source: AppErrorSource, connectionKey: String) {
        reportConnectionRecovered(source, connectionKey)
        deactivateFatal(backendFailureKey(source, connectionKey))
    }

    fun reportBackendFailure(
        source: AppErrorSource,
        throwable: Throwable,
        operation: String,
        connectionKey: String = operation,
    ) {
        if (throwable.isConnectionFailure()) {
            reportConnectionFailure(source, throwable, operation, connectionKey)
        } else {
            reportFatal(throwable, operation, backendFailureKey(source, connectionKey))
        }
    }

    fun reportFatal(throwable: Throwable, operation: String, failureKey: String? = null) {
        if (failureKey != null && !activateFatal(failureKey)) {
            logger.debug { "$operation still requires user attention: failureKey=$failureKey" }
            return
        }
        logger.error(throwable) { "$operation failed and requires user attention" }
        val enqueued = enqueue(
            event = AppErrorEvent(AppErrorSource.APPLICATION, AppErrorPresentation.DIALOG),
            deduplicationKey = failureKey ?: "fatal:$operation",
        )
        if (!enqueued && failureKey != null) deactivateFatal(failureKey)
    }

    private fun enqueue(event: AppErrorEvent, deduplicationKey: String): Boolean {
        while (true) {
            val current = pendingEvents.value
            if (current.any { it.deduplicationKey == deduplicationKey }) return false
            val pendingFatalCount = current.count { it.event.presentation == AppErrorPresentation.DIALOG }
            if (event.presentation == AppErrorPresentation.DIALOG && pendingFatalCount >= MAX_PENDING_FATAL_EVENTS) {
                logger.log(LogLevel.ERROR, "Fatal app error event queue is full: pending=$pendingFatalCount")
                return false
            }
            val pending = PendingEvent(event.copy(eventId = allocateEventId()), deduplicationKey)
            if (pendingEvents.compareAndSet(current, current + pending)) return true
        }
    }

    private fun allocateEventId(): Long {
        while (true) {
            val current = nextEventId.value
            if (nextEventId.compareAndSet(current, current + 1)) return current
        }
    }

    private fun activate(key: ConnectionFailureKey): ActivationResult {
        while (true) {
            val current = activeConnectionFailures.value
            if (key in current) return ActivationResult(keyAdded = false, sourceBecameUnavailable = false)
            if (activeConnectionFailures.compareAndSet(current, current + key)) {
                return ActivationResult(
                    keyAdded = true,
                    sourceBecameUnavailable = current.none { it.source == key.source },
                )
            }
        }
    }

    private fun deactivate(key: ConnectionFailureKey): RecoveryResult {
        while (true) {
            val current = activeConnectionFailures.value
            if (key !in current) return RecoveryResult(keyRemoved = false, sourceBecameAvailable = false)
            val updated = current - key
            if (activeConnectionFailures.compareAndSet(current, updated)) {
                return RecoveryResult(
                    keyRemoved = true,
                    sourceBecameAvailable = updated.none { it.source == key.source },
                )
            }
        }
    }

    private fun activateFatal(failureKey: String): Boolean {
        while (true) {
            val current = activeFatalFailures.value
            if (failureKey in current) return false
            if (activeFatalFailures.compareAndSet(current, current + failureKey)) return true
        }
    }

    private fun deactivateFatal(failureKey: String) {
        while (true) {
            val current = activeFatalFailures.value
            if (failureKey !in current) return
            if (activeFatalFailures.compareAndSet(current, current - failureKey)) return
        }
    }

    private fun backendFailureKey(source: AppErrorSource, connectionKey: String): String =
        "backend:$source:$connectionKey"

    private data class ActivationResult(val keyAdded: Boolean, val sourceBecameUnavailable: Boolean)
    private data class RecoveryResult(val keyRemoved: Boolean, val sourceBecameAvailable: Boolean)
    private data class PendingEvent(val event: AppErrorEvent, val deduplicationKey: String)

    private companion object {
        // Connection events are independently bounded by AppErrorSource and never displaced by fatal bursts.
        const val MAX_PENDING_FATAL_EVENTS = 32
    }
}

fun Throwable.isConnectionFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is GrpcStatusException) {
            return current.status.statusCode in TRANSIENT_GRPC_STATUS_CODES
        }
        current = current.cause
    }
    return false
}

private val TRANSIENT_GRPC_STATUS_CODES = setOf(
    GrpcStatusCode.DEADLINE_EXCEEDED,
    GrpcStatusCode.RESOURCE_EXHAUSTED,
    GrpcStatusCode.UNAVAILABLE,
)
