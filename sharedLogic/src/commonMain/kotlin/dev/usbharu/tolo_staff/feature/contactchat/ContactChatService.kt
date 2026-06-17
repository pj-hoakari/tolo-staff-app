package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.flow.Flow

interface ContactChatService {
    fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>>

    fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>>

    suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String)
}
