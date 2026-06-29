package dev.usbharu.tolo_staff.streaming

actual fun defaultOperationsStreamingConfig(): OperationsStreamingConfig = OperationsStreamingConfig(
    enabled = true,
    host = "10.0.2.2",
    port = 8081,
)
