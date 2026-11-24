package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.NoOpEventRepository
import dev.usbharu.tolo_staff.feature.appshell.NoOpReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.OperationsReadMode
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.defaultOperationsReadMode
import dev.usbharu.tolo_staff.streaming.defaultOperationsStreamingConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { defaultOperationsReadMode() }
    single { defaultOperationsPollingConfig() }
    single {
        val config = get<OperationsPollingConfig>()
        GrpcCommunicationClient(
            host = config.host,
            port = config.port,
            metadataProvider = get(),
        )
    }
    single<OperationsChangeNotifier> { FirestoreOperationsChangeNotifier(defaultOperationsStreamingConfig()) }
    single<ReportRepository> { NoOpReportRepository() }
    single<EventRepository> { NoOpEventRepository() }
    single<AssignmentStatusService> {
        val config = get<OperationsPollingConfig>()
        GrpcAssignmentStatusService(
            grpcClient = get(),
            eventId = config.eventId,
            tenantId = config.tenantId,
        )
    }
    single<OperationsStreamDataSource> {
        check(get<OperationsReadMode>() == OperationsReadMode.FIRESTORE)
        FirestoreOperationsStreamDataSource(
            config = defaultOperationsStreamingConfig(),
            grpcClient = get(),
            pollingConfig = get(),
        )
    }
    single<ContactChatService> { GrpcContactChatService(dataSource = get(), grpcClient = get()) }
}
