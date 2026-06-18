package dev.usbharu.tolo_staff.feature.appshell

data class AppShellMapState(
    val venueName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val latitudeDelta: Double = 0.0,
    val longitudeDelta: Double = 0.0
)
