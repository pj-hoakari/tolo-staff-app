package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel
import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper : KoinComponent {
    fun getAppShellViewModel(): AppShellViewModel = get()

    fun getContactChatViewModel(): ContactChatViewModel = get()

    fun getSampleViewModel(): SampleViewModel = get()
}
