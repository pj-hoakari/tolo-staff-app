package dev.usbharu.tolo_staff.streaming

interface CurrentStaffProvider {
    val currentStaffId: String
}

class FixedCurrentStaffProvider(
    override val currentStaffId: String = "tanaka"
) : CurrentStaffProvider
