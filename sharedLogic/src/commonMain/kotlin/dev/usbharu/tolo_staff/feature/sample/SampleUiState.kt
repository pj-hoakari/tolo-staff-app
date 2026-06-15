package dev.usbharu.tolo_staff.feature.sample

data class SampleUiState(
    val message: String = "KMP is ready",
    val tapCount: Int = 0,
    val lastAction: String? = null
)
