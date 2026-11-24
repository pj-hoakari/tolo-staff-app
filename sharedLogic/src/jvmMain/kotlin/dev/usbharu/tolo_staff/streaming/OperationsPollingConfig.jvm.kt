package dev.usbharu.tolo_staff.streaming

actual fun defaultOperationsPollingConfig(): OperationsPollingConfig = OperationsPollingConfig(
    host = "localhost",
    port = 8080,
)
