package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * The current operation API does not expose a change-head RPC, so this notifier
 * periodically emits a synthetic change to keep gRPC-backed streams refreshed.
 */
class GrpcOperationsChangeNotifier(
    private val config: OperationsPollingConfig = defaultOperationsPollingConfig(),
) : OperationsChangeNotifier {
    private val logger = AppLogger.withTag("GrpcOperationsChangeNotifier")

    init {
        logger.debug {
            "Change-head RPC is unavailable; using periodic refresh: intervalMillis=${config.intervalMillis}"
        }
    }

    override fun observeGlobal(): Flow<OperationsChangeHead> = periodicChange("global")

    override fun observeStaff(staffId: String): Flow<OperationsChangeHead> =
        periodicChange("staff:$staffId")

    private fun periodicChange(label: String): Flow<OperationsChangeHead> = flow {
        var version = INITIAL_VERSION
        while (currentCoroutineContext().isActive) {
            delay(config.intervalMillis)
            version += 1
            logger.trace { "Emitting periodic refresh: label=$label, version=$version" }
            emit(
                OperationsChangeHead(
                    version = version,
                    entityType = OperationEntityType.UNKNOWN,
                    reason = "periodic.refresh",
                    operation = OperationChangeType.UPDATED,
                )
            )
        }
    }

    private companion object {
        const val INITIAL_VERSION = 0L
    }
}
