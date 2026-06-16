package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.GrpcContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import dev.usbharu.tolo_staff.streaming.CurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.FirestoreOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.FixedCurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.defaultOperationsStreamingConfig
import org.koin.core.context.startKoin
import org.koin.dsl.module

val coreModule = module {
    single<CurrentStaffProvider> { FixedCurrentStaffProvider() }
    single<OperationsStreamDataSource> {
        val config = defaultOperationsStreamingConfig()
        if (config.enabled) {
            FirestoreOperationsStreamDataSource(config)
        } else {
            NoOpOperationsStreamDataSource()
        }
    }
    single<OperationsOverviewRepository> { OperationsOverviewRepositoryImpl(dataSource = get()) }
    single<ContactChatService> { GrpcContactChatService(dataSource = get()) }
    factory {
        AppShellViewModel(
            overviewRepository = get(),
            currentStaffProvider = get(),
        )
    }
    factory { ContactChatViewModel(service = get()) }
    factory { SampleViewModel() }
}

object KoinInitializer {
    private var started = false

    fun start() {
        if (started) {
            return
        }

        startKoin {
            modules(coreModule)
        }
        started = true
    }
}
