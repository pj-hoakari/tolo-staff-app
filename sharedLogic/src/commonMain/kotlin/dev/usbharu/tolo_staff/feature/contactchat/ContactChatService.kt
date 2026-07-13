package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.flow.Flow

enum class ChatThreadTargetKind { DIRECT, LOCATION }

data class ChatThreadTarget(
    val id: String,
    val title: String,
    val kind: ChatThreadTargetKind,
)

interface ContactChatService {
    fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>>

    fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>>

    suspend fun sendMessage(threadId: String, body: String)

    suspend fun createThread(target: ChatThreadTarget): ChatRoom
}
