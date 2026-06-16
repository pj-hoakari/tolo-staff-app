package dev.usbharu.tolo_staff.feature.contactchat

interface ContactChatService {
    suspend fun listRooms(currentStaffId: String): List<ChatRoom>

    suspend fun listMessages(roomId: String, currentStaffId: String): List<ChatMessage>

    suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String)
}
