package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.error.AppErrorReporter
import dev.usbharu.tolo_staff.error.AppErrorSource
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
    private val errorReporter: AppErrorReporter = AppErrorReporter(),
) {
    fun <T> create(connectionKey: String, fetch: suspend () -> List<T>): Flow<List<T>> = flow {
        var hasEmitted = false
        while (currentCoroutineContext().isActive) {
            val result = try {
                withContext(Dispatchers.Default) { fetch() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                errorReporter.reportBackendFailure(
                    AppErrorSource.GRPC,
                    throwable,
                    "Polling gRPC fetch: connectionKey=$connectionKey",
                    connectionKey,
                )
                null
            }
            if (result != null) {
                errorReporter.reportBackendRecovered(AppErrorSource.GRPC, connectionKey)
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
