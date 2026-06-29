package dev.usbharu.tolo_staff.feature.contactchat

data class ChatRoom(
    val id: String,
    val title: String,
    val lastMessage: String? = null,
    val unreadCount: Int = 0
)
