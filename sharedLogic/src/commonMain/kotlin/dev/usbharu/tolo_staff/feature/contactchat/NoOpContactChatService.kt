package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoOpContactChatService : ContactChatService {
    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> = flowOf(emptyList())

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> = flowOf(emptyList())

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        error("Contact chat service is not available on this platform")
    }
}
