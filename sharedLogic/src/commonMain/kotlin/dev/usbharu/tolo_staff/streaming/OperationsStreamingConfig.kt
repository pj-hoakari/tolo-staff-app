package dev.usbharu.tolo_staff.streaming

data class OperationsStreamingConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val projectId: String = "tolo-communication-poc",
    val databaseId: String? = null,
)

expect fun defaultOperationsStreamingConfig(): OperationsStreamingConfig
