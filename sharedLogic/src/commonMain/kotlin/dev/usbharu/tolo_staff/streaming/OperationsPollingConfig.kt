package dev.usbharu.tolo_staff.streaming

data class OperationsPollingConfig(
    val host: String,
    val port: Int,
    val intervalMillis: Long = 5_000,
)

expect fun defaultOperationsPollingConfig(): OperationsPollingConfig
