package dev.usbharu.tolo_staff.streaming

data class OperationsStreamingConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val authPort: Int = 9099,
    val projectId: String = "demo-tolo",
)

expect fun defaultOperationsStreamingConfig(): OperationsStreamingConfig
