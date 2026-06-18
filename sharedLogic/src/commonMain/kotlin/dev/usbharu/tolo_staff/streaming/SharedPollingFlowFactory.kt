package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal class SharedPollingFlowFactory(
    private val intervalMillis: Long,
) {
    private val logger = AppLogger.withTag("SharedPollingFlowFactory")

    fun <T> create(fetch: suspend () -> List<T>): Flow<List<T>> = flow {
        while (currentCoroutineContext().isActive) {
            val result = try {
                withContext(Dispatchers.Default) { fetch() }
            } catch (throwable: Throwable) {
                logger.warn(throwable) { "Polling fetch failed; terminating current flow" }
                throw throwable
            }
            emit(result)
            delay(intervalMillis)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
