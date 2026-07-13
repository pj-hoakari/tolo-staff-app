package dev.usbharu.tolo_staff.streaming

data class OperationsPollingConfig(
    val intervalMillis: Long = 5_000,
)

fun defaultOperationsPollingConfig(): OperationsPollingConfig = OperationsPollingConfig()
