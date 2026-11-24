package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.streaming.FirestoreLatestDeliveryIds
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.RealtimeDeliveryFeed
import dev.usbharu.tolo_staff.streaming.RealtimeDeliveryRepository
import dev.usbharu.tolo_staff.streaming.RealtimeFeedEvent
import dev.usbharu.tolo_staff.streaming.RealtimeOperationsChangeNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    includes(grpcOperationsModule(changeNotifier = { RealtimeOperationsChangeNotifier(get<SharedFlow<RealtimeFeedEvent>>()) }))
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<SharedFlow<RealtimeFeedEvent>>(createdAtStart = true) {
        val client = get<GrpcCommunicationClient>()
        RealtimeDeliveryFeed(
            realtime = client.realtimeFetchService,
            deliveryCoordination = client.deliveryCoordinationService,
            scope = get(),
            latestDeliveryIds = FirestoreLatestDeliveryIds(client.realtimeFetchService)::observe,
        ).events().shareIn(get(), SharingStarted.Eagerly)
    }
    single(createdAtStart = true) { RealtimeDeliveryRepository(events = get<SharedFlow<RealtimeFeedEvent>>(), scope = get()) }
}
