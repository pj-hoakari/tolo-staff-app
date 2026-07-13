package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.NoOpGrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.toSessionContext
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.FixedCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun coreModule(config: AppRuntimeConfig, metadataProvider: GrpcMetadataProvider) = module {
    single { config }
    single { config.toSessionContext() }
    single { metadataProvider }
    single<CurrentStaffSession> { FixedCurrentStaffSession(staffId = config.staffId, dataSource = get()) }
    single<OperationsOverviewRepository> { OperationsOverviewRepositoryImpl(dataSource = get(), eventRepository = get()) }
    factory {
        AppShellViewModel(
            overviewRepository = get(),
            dataSource = get(),
            contactChatService = get(),
            currentStaffSession = get(),
            reportRepository = get(),
            assignmentStatusService = get(),
        )
    }
    factory { ContactChatViewModel(service = get(), currentStaffSession = get()) }
    factory { SampleViewModel() }
}

object KoinInitializer {
    private val logger = AppLogger.withTag("KoinInitializer")
    private var started = false

    fun start(config: AppRuntimeConfig) {
        start(config, NoOpGrpcMetadataProvider)
    }

    fun start(config: AppRuntimeConfig, metadataProvider: GrpcMetadataProvider) {
        if (started) {
            logger.debug { "start skipped because Koin is already initialized" }
            return
        }

        logger.info { "Starting Koin modules for shared logic" }
        val validatedConfig = config.requireValid()
        startKoin {
            modules(coreModule(validatedConfig, metadataProvider), platformModule())
        }
        started = true
        logger.info { "Koin started successfully" }
    }
}
