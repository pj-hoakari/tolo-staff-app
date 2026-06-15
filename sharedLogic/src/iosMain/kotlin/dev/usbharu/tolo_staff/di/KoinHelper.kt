package dev.usbharu.tolo_staff.di

import dev.usbharu.tolo_staff.feature.sample.SampleViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper : KoinComponent {
    fun getSampleViewModel(): SampleViewModel = get()
}
