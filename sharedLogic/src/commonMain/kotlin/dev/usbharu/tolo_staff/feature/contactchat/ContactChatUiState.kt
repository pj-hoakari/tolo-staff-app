package dev.usbharu.tolo_staff.feature.contactchat

data class ContactChatUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val selectedRoomId: String? = null,
    val selectedRoomTitle: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draftText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
