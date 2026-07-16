package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig

expect class OperationsFirebaseBootstrap() {
    fun initialize(
        context: Any? = null,
        config: AppRuntimeConfig,
    )
}
