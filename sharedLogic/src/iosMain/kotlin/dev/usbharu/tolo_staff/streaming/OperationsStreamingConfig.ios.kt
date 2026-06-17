package dev.usbharu.tolo_staff.streaming

actual fun defaultOperationsStreamingConfig(): OperationsStreamingConfig = OperationsStreamingConfig(
    enabled = true,
    host = "127.0.0.1",
    port = 8081,
)
