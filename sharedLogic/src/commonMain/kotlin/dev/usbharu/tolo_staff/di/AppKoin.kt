package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module

val coreModule = module {
    single<CurrentStaffSession> { MockCurrentStaffSession(dataSource = get()) }
    single<OperationsOverviewRepository> { OperationsOverviewRepositoryImpl(dataSource = get()) }
    factory {
        AppShellViewModel(
            overviewRepository = get(),
            dataSource = get(),
            contactChatService = get(),
            currentStaffSession = get(),
        )
    }
    factory { ContactChatViewModel(service = get(), currentStaffSession = get()) }
    factory { SampleViewModel() }
}

object KoinInitializer {
    private val logger = AppLogger.withTag("KoinInitializer")
    private var started = false

    fun start() {
        if (started) {
            logger.debug { "start skipped because Koin is already initialized" }
            return
        }

        logger.info { "Starting Koin modules for shared logic" }
        startKoin {
            modules(coreModule, platformModule())
        }
        started = true
        logger.info { "Koin started successfully" }
    }
}
