package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.error.AppErrorReporter
import dev.usbharu.tolo_staff.error.AppStartupErrorInjector
import dev.usbharu.tolo_staff.error.AppStartupErrorScenario
import dev.usbharu.tolo_staff.error.NoOpAppStartupErrorInjector
import dev.usbharu.tolo_staff.error.ScenarioAppStartupErrorInjector
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.auth.AuthGrpcMetadataProvider
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.feature.auth.AuthViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.logging.AppLogConfig
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.NoOpGrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.toSessionContext
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.AuthenticatedCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.FixedCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun coreModule(
    config: AppRuntimeConfig,
    metadataProvider: GrpcMetadataProvider,
    startupErrorInjector: AppStartupErrorInjector = NoOpAppStartupErrorInjector,
) = module {
    single { config }
    single { config.toSessionContext() }
    single { AppErrorReporter() }
    single { startupErrorInjector }
    single { AuthRepository(get()) }
    single<GrpcMetadataProvider> {
        when {
            !config.oidcLoginEnabled -> NoOpGrpcMetadataProvider
            metadataProvider !== NoOpGrpcMetadataProvider -> metadataProvider
            else -> AuthGrpcMetadataProvider(get())
        }
    }
    single<CurrentStaffSession> {
        if (config.oidcLoginEnabled) {
            AuthenticatedCurrentStaffSession(sessionContext = get(), dataSource = get())
        } else {
            FixedCurrentStaffSession(staffId = config.staffId, dataSource = get())
        }
    }
    single<OperationsOverviewRepository> { OperationsOverviewRepositoryImpl(dataSource = get(), eventRepository = get()) }
    factory {
        AppShellViewModel(
            overviewRepository = get(),
            dataSource = get(),
            contactChatService = get(),
            currentStaffSession = get(),
            reportRepository = get(),
            assignmentStatusService = get(),
            errorReporter = get(),
            startupErrorInjector = get(),
        )
    }
    factory { ContactChatViewModel(service = get(), currentStaffSession = get()) }
    factory { SampleViewModel() }
    factory { AuthViewModel(get()) }
}

object KoinInitializer {
    private val logger = AppLogger.withTag("KoinInitializer")
    private var started = false

    fun start(config: AppRuntimeConfig) {
        start(config, NoOpGrpcMetadataProvider, NoOpAppStartupErrorInjector)
    }

    fun start(config: AppRuntimeConfig, metadataProvider: GrpcMetadataProvider) {
        start(config, metadataProvider, NoOpAppStartupErrorInjector)
    }

    fun start(config: AppRuntimeConfig, startupErrorScenario: AppStartupErrorScenario) {
        start(
            config,
            NoOpGrpcMetadataProvider,
            ScenarioAppStartupErrorInjector(startupErrorScenario),
        )
    }

    private fun start(
        config: AppRuntimeConfig,
        metadataProvider: GrpcMetadataProvider,
        startupErrorInjector: AppStartupErrorInjector,
    ) {
        if (started) {
            logger.debug { "start skipped because Koin is already initialized" }
            return
        }

        AppLogConfig.configure(config.isDebugBuild)
        logger.info { "Starting Koin modules for shared logic" }
        val validatedConfig = config.requireValid()
        startKoin {
            modules(coreModule(validatedConfig, metadataProvider, startupErrorInjector), platformModule())
        }
        started = true
        logger.info { "Koin started successfully" }
    }
}
