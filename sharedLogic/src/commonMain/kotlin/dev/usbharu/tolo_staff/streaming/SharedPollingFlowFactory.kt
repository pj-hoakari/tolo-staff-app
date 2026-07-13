package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.CancellationException
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
        var hasEmitted = false
        while (currentCoroutineContext().isActive) {
            val result = try {
                withContext(Dispatchers.Default) { fetch() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logger.warn(throwable) { "Polling fetch failed; keeping the flow active for retry" }
                null
            }
            if (result != null) {
                emit(result)
                hasEmitted = true
            } else if (!hasEmitted) {
                emit(emptyList())
                hasEmitted = true
            }
            delay(intervalMillis)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
