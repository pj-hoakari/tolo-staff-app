package dev.usbharu.tolo_staff.feature.contactchat

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderName: String,
    val body: String,
    val timeLabel: String? = null,
    val isFromCurrentUser: Boolean = false,
    val isSystemEvent: Boolean = false
)
