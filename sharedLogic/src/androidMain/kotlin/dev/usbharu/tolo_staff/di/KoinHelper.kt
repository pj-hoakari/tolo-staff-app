package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.feature.auth.AuthViewModel
import dev.usbharu.tolo_staff.streaming.FirebaseSessionController
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper : KoinComponent {
    fun getAppShellViewModel(): AppShellViewModel = get()

    fun getContactChatViewModel(): ContactChatViewModel = get()

    fun getSampleViewModel(): SampleViewModel = get()

    fun getAuthViewModel(): AuthViewModel = get()

    fun getAuthRepository(): AuthRepository = get()

    fun getFirebaseSessionController(): FirebaseSessionController = get()

    fun getRuntimeConfig(): AppRuntimeConfig = get()
}
