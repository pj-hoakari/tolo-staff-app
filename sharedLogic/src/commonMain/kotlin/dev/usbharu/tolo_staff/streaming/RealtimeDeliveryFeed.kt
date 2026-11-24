package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.write
import kotlinx.rpc.internal.utils.ExperimentalRpcApi
import kotlinx.rpc.internal.utils.InternalRpcApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tolo.operation.v1.DeliveryCoordinationService
import tolo.operation.v1.DeliveryPayload
import tolo.operation.v1.DeliveryPayloadInternal
import tolo.operation.v1.UpdateConnectionStateRequest
import tolo.operation.v1.invoke
import tolo.realtime.v1.Envelope
import tolo.realtime.v1.FetchDeliveriesRequest
import tolo.realtime.v1.RealtimeFetchService
import tolo.realtime.v1.invoke

data class RealtimeDelivery(
    val deliveryId: String,
    val sentAt: String,
    val payload: DeliveryPayload,
)

sealed interface RealtimeFeedEvent {
    data object Connected : RealtimeFeedEvent

    data class Delivered(val delivery: RealtimeDelivery) : RealtimeFeedEvent
}

class RealtimeDeliveryFeed @OptIn(ExperimentalUuidApi::class) constructor(
    private val realtime: RealtimeFetchService,
    private val deliveryCoordination: DeliveryCoordinationService,
    private val scope: OperationsScope,
    private val latestDeliveryIds: (eventId: String) -> Flow<String>,
    private val deviceId: String = Uuid.random().toString(),
    private val retryDelay: kotlin.time.Duration = 5.seconds,
) {
    private val logger = AppLogger.withTag("RealtimeDeliveryFeed")

    fun events(): Flow<RealtimeFeedEvent> {
        if (scope.eventId.isBlank() || scope.tenantId.isBlank() || scope.accessToken.isBlank()) {
            logger.warn { "Realtime delivery feed is disabled because the operations scope is incomplete" }
            return emptyFlow()
        }
        var cursor: String? = null
        return flow {
            updateConnectionState(connected = true)
            emit(RealtimeFeedEvent.Connected)
            try {
                latestDeliveryIds(scope.eventId).distinctUntilChanged().collect { latest ->
                    while (cursor != latest) {
                        val response = realtime.FetchDeliveries(
                            FetchDeliveriesRequest {
                                eventId = scope.eventId
                                resumeAfterDeliveryId = cursor
                            }
                        )
                        response.envelopes.forEach { emit(RealtimeFeedEvent.Delivered(it.toRealtimeDelivery())) }
                        val next = response.latestDeliveryId
                        if (next.isBlank() || next == cursor) break
                        cursor = next
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    runCatching { updateConnectionState(connected = false) }
                        .onFailure { logger.warn(it) { "Failed to report disconnection" } }
                }
            }
        }.retryWhen { cause, attempt ->
            logger.warn(cause) { "Realtime delivery feed failed: attempt=$attempt, cursor=$cursor" }
            delay(retryDelay)
            true
        }
    }

    private suspend fun updateConnectionState(connected: Boolean) {
        val now = Clock.System.now()
        deliveryCoordination.UpdateConnectionState(
            UpdateConnectionStateRequest {
                userId = subjectOf(scope.accessToken)
                deviceId = this@RealtimeDeliveryFeed.deviceId
                this.connected = connected
                at = Timestamp {
                    seconds = now.epochSeconds
                    nanos = now.nanosecondsOfSecond
                }
                eventId = scope.eventId
                tenantId = scope.tenantId
            }
        )
        logger.info { "Reported connection state: connected=$connected, deviceId=$deviceId" }
    }
}

internal fun Envelope.toRealtimeDelivery(): RealtimeDelivery = RealtimeDelivery(
    deliveryId = deliveryId,
    sentAt = sentAt.toIsoString(),
    payload = decodeDeliveryPayload(payload),
)

@OptIn(InternalRpcApi::class, ExperimentalRpcApi::class)
internal fun decodeDeliveryPayload(bytes: ByteString): DeliveryPayload =
    DeliveryPayloadInternal.MARSHALLER.decode(Buffer().apply { write(bytes) }, null)

@OptIn(ExperimentalEncodingApi::class)
internal fun subjectOf(jwt: String): String {
    val payload = jwt.split('.').getOrNull(1) ?: return ""
    val json = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload).decodeToString()
    return Json.parseToJsonElement(json).jsonObject["sub"]?.jsonPrimitive?.content.orEmpty()
}

class RealtimeDeliveryRepository(events: Flow<RealtimeFeedEvent>, scope: CoroutineScope) {
    val deliveries: StateFlow<List<RealtimeDelivery>> = events
        .filterIsInstance<RealtimeFeedEvent.Delivered>()
        .runningFold(emptyList<RealtimeDelivery>()) { received, event ->
            (received + event.delivery).takeLast(MAX_RETAINED_DELIVERIES)
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private companion object {
        const val MAX_RETAINED_DELIVERIES = 100
    }
}
