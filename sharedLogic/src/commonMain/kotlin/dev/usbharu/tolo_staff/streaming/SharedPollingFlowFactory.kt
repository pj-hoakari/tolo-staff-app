package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class SharedPollingFlowFactory(
    private val intervalMillis: Long,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun <T> create(fetch: suspend () -> List<T>): Flow<List<T>> = flow {
        var latestValue: List<T>? = null
        while (currentCoroutineContext().isActive) {
            val nextValue = try {
                withContext(Dispatchers.Default) { fetch() }
            } catch (_: Throwable) {
                latestValue
            }
            if (nextValue != null) {
                latestValue = nextValue
                emit(nextValue)
            }
            delay(intervalMillis)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(
                stopTimeoutMillis = intervalMillis,
            ),
            replay = 1,
        )
}
