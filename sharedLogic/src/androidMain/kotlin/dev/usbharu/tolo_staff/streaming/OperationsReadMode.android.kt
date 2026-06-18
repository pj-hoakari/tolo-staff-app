package dev.usbharu.tolo_staff.streaming

actual fun defaultOperationsReadMode(): OperationsReadMode = OperationsReadMode.FIRESTORE

actual fun defaultOperationsPollingConfig(): OperationsPollingConfig = OperationsPollingConfig(
    host = "10.0.2.2",
    port = 8080,
)
