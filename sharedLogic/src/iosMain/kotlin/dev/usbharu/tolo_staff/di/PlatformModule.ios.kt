package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.EventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcEventRepository
import dev.usbharu.tolo_staff.feature.appshell.GrpcReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.GrpcOperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.OperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.PollingOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsPollingConfig
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { defaultOperationsPollingConfig() }
    single {
        val config = get<AppRuntimeConfig>()
        GrpcCommunicationClient(config.grpcHost, config.grpcPort, get<GrpcMetadataProvider>(), get<AuthRepository>())
    }
    single<ReportRepository> { GrpcReportRepository(get(), get(), pollingIntervalMillis = get<dev.usbharu.tolo_staff.streaming.OperationsPollingConfig>().intervalMillis) }
    single<EventRepository> { GrpcEventRepository(get(), get()) }
    single<AssignmentStatusService> { GrpcAssignmentStatusService(get(), get()) }
    single { GrpcOperationsPollingRemoteDataSource(get(), get()) }
    single<OperationsPollingRemoteDataSource> { get<GrpcOperationsPollingRemoteDataSource>() }
    single<OperationsStreamDataSource> { PollingOperationsStreamDataSource(get(), get()) }
    single<ContactChatService> { GrpcContactChatService(get(), get(), get()) }
}
