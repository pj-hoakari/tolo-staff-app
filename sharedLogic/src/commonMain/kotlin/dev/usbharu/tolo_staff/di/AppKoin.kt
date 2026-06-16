package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val coreModule = module {
    factory { AppShellViewModel() }
    factory { ContactChatViewModel() }
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
