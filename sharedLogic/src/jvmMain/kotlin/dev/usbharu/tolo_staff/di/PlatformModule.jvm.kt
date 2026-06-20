package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.NoOpAssignmentStatusService
import dev.usbharu.tolo_staff.feature.appshell.NoOpReportRepository
import dev.usbharu.tolo_staff.feature.appshell.ReportRepository
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.NoOpContactChatService
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.OperationsReadMode
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsReadMode
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { defaultOperationsReadMode() }
    single<OperationsStreamDataSource> { NoOpOperationsStreamDataSource() }
    single<ContactChatService> { NoOpContactChatService() }
    single<ReportRepository> { NoOpReportRepository() }
    single<AssignmentStatusService> { NoOpAssignmentStatusService() }
}
