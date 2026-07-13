package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcEventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.streaming.OperationsStreamingConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        val config = get<AppRuntimeConfig>()
        OperationsStreamingConfig(
            enabled = config.firestoreEnabled,
            host = config.firestoreHost,
            port = config.firestorePort,
            projectId = config.firestoreProjectId,
            databaseId = config.firestoreDatabaseId,
        )
    }
    single {
        val config = get<AppRuntimeConfig>()
        GrpcCommunicationClient(config.grpcHost, config.grpcPort, get<GrpcMetadataProvider>())
    }
    single<OperationsChangeNotifier> {
        FirestoreOperationsChangeNotifier(get(), get<AppSessionContext>())
    }
    single<ReportRepository> { GrpcReportRepository(get(), get(), changeNotifier = get()) }
    single<EventRepository> { GrpcEventRepository(get(), get()) }
    single<AssignmentStatusService> { GrpcAssignmentStatusService(get(), get()) }
    single<OperationsStreamDataSource> {
        FirestoreOperationsStreamDataSource(
            config = get(),
            grpcClient = get(),
            session = get(),
        )
    }
    single<ContactChatService> { GrpcContactChatService(get(), get(), get()) }
}
