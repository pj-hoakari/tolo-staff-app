package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.GrpcReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationsReadMode
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsReadMode
import dev.usbharu.tolo_staff.streaming.defaultOperationsStreamingConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { defaultOperationsReadMode() }
    single { GrpcCommunicationClient(host = dev.usbharu.tolo_staff.feature.contactchat.contactChatServerHost, port = 8080) }
    single<ReportRepository> { GrpcReportRepository(grpcClient = get()) }
    single<AssignmentStatusService> { GrpcAssignmentStatusService(grpcClient = get()) }
    single<OperationsStreamDataSource> {
        check(get<OperationsReadMode>() == OperationsReadMode.FIRESTORE)
        FirestoreOperationsStreamDataSource(defaultOperationsStreamingConfig())
    }
    single<ContactChatService> { GrpcContactChatService(dataSource = get(), grpcClient = get()) }
}
