package dev.usbharu.tolo_staff.feature.appshell

import com.google.protobuf.kotlin.Timestamp
import dev.usbharu.tolo.communication.v1.Event
import dev.usbharu.tolo.communication.v1.GetEventRequest
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.toProtoScope
import dev.usbharu.tolo_staff.streaming.toIsoString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

interface EventRepository {
    fun observeCurrentEvent(): Flow<OperationEvent>
}

data class OperationEvent(
    val eventId: String = "",
    val name: String = "",
    val venueName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val latitudeDelta: Double = 0.0,
    val longitudeDelta: Double = 0.0,
    val startTimeIso: String? = null,
    val endTimeIso: String? = null,
)

class NoOpEventRepository : EventRepository {
    override fun observeCurrentEvent(): Flow<OperationEvent> = flowOf(OperationEvent())
}

class GrpcEventRepository(
    private val grpcClient: GrpcCommunicationClient,
    private val session: AppSessionContext,
) : EventRepository {
    private val logger = AppLogger.withTag("GrpcEventRepository")

    override fun observeCurrentEvent(): Flow<OperationEvent> = flow {
        val event = runCatching {
            grpcClient.referenceService.GetEvent(
                GetEventRequest { scope = session.toProtoScope() }
            ).toOperationEvent()
        }.getOrElse { error ->
            logger.warn(error) { "Failed to fetch current event via gRPC; falling back to default" }
            OperationEvent()
        }
        logger.debug {
            "Fetched current event via gRPC: eventId=${event.eventId}, name=${event.name}, venue=${event.venueName}"
        }
        emit(event)
    }
}

private fun Event.toOperationEvent(): OperationEvent = OperationEvent(
    eventId = scope.eventId.value,
    name = name,
    startTimeIso = startsAt.toIsoIfSet(),
    endTimeIso = endsAt.toIsoIfSet(),
)

private fun Timestamp?.toIsoIfSet(): String? {
    val timestamp = this ?: return null
    if (timestamp.seconds == 0L && timestamp.nanos == 0) return null
    return timestamp.toIsoString().ifBlank { null }
}
