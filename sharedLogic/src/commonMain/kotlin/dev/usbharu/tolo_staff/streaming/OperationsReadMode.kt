package dev.usbharu.tolo_staff.streaming

enum class OperationsReadMode {
    FIRESTORE,
    POLLING,
    GRPC,
}

data class OperationsPollingConfig(
    val host: String,
    val port: Int,
    val intervalMillis: Long = 5_000,
    val eventId: String = "",
    val tenantId: String = "",
)

expect fun defaultOperationsReadMode(): OperationsReadMode

expect fun defaultOperationsPollingConfig(): OperationsPollingConfig
