package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import tolo.operation.v1.DeliveryPayload

class RealtimeOperationsChangeNotifier(events: Flow<RealtimeFeedEvent>) : OperationsChangeNotifier {
    private val changes: Flow<OperationsChangeHead> = events.mapNotNull { it.toChangeHead() }

    override fun observeGlobal(): Flow<OperationsChangeHead> = changes

    override fun observeStaff(staffId: String): Flow<OperationsChangeHead> = changes
}

internal fun RealtimeFeedEvent.toChangeHead(): OperationsChangeHead? = when (this) {
    RealtimeFeedEvent.Connected -> OperationsChangeHead(reason = "realtime.connected")
    is RealtimeFeedEvent.Delivered -> when (delivery.payload.kind) {
        is DeliveryPayload.Kind.StaffMessage -> OperationsChangeHead(
            entityType = OperationEntityType.INSTRUCTION,
            entityId = delivery.deliveryId,
            reason = "realtime.delivery",
        )

        is DeliveryPayload.Kind.FlowProposals,
        is DeliveryPayload.Kind.LineGuidance,
        null,
        -> null
    }
}
