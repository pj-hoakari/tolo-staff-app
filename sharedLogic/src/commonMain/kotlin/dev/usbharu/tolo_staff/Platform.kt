package dev.usbharu.tolo_staff

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform