package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.NoOpEventRepository
import dev.usbharu.tolo_staff.feature.appshell.NoOpReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.GrpcBackedOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.GrpcOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.GrpcOperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.OperationsScope
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsPollingConfig
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun grpcOperationsModule(
    changeNotifier: Scope.() -> OperationsChangeNotifier = { GrpcOperationsChangeNotifier(config = get()) },
): Module = module {
    single { defaultOperationsPollingConfig() }
    single {
        val config = get<OperationsPollingConfig>()
        GrpcCommunicationClient(
            host = config.host,
            port = config.port,
            metadataProvider = get(),
        )
    }
    single<OperationsChangeNotifier> { changeNotifier() }
    single<ReportRepository> { NoOpReportRepository() }
    single<EventRepository> { NoOpEventRepository() }
    single<AssignmentStatusService> {
        val scope = get<OperationsScope>()
        GrpcAssignmentStatusService(
            grpcClient = get(),
            eventId = scope.eventId,
            tenantId = scope.tenantId,
        )
    }
    single<OperationsStreamDataSource> {
        val scope = get<OperationsScope>()
        GrpcBackedOperationsStreamDataSource(
            remoteDataSource = GrpcOperationsPollingRemoteDataSource(
                grpcClient = get(),
                eventId = scope.eventId,
                tenantId = scope.tenantId,
            ),
            changeNotifier = get(),
        )
    }
    single<ContactChatService> { GrpcContactChatService(dataSource = get(), grpcClient = get()) }
}
