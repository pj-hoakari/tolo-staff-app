package dev.usbharu.tolo_staff.feature.appshell

import kotlinx.coroutines.flow.Flow
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
