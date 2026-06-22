package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcEventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.PollingContactChatService
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.GrpcOperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.NoOpOperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsChangeNotifier
import dev.usbharu.tolo_staff.streaming.OperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.OperationsReadMode
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.PollingOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.defaultOperationsReadMode
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { defaultOperationsReadMode() }
    single { defaultOperationsPollingConfig() }
    single {
        val config = get<dev.usbharu.tolo_staff.streaming.OperationsPollingConfig>()
        GrpcCommunicationClient(host = config.host, port = config.port)
    }
    single<OperationsChangeNotifier> { NoOpOperationsChangeNotifier() }
    single<ReportRepository> { GrpcReportRepository(grpcClient = get(), changeNotifier = get()) }
    single<EventRepository> { GrpcEventRepository(grpcClient = get()) }
    single<AssignmentStatusService> { GrpcAssignmentStatusService(grpcClient = get()) }
    single<OperationsPollingRemoteDataSource> { GrpcOperationsPollingRemoteDataSource(grpcClient = get()) }
    single<OperationsStreamDataSource> {
        check(get<OperationsReadMode>() == OperationsReadMode.POLLING)
        PollingOperationsStreamDataSource(remoteDataSource = get(), config = get())
    }
    single<ContactChatService> { PollingContactChatService(remoteDataSource = get(), config = get(), grpcClient = get()) }
}
