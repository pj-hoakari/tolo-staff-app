package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class ContactChatViewModel(
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<ContactChatUiState, Unit>(
    initialState = ContactChatUiState(rooms = initialRooms),
    coroutineContext = coroutineContext
) {
    private val roomMessages = initialMessages
        .groupBy { it.roomId }
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    fun onRoomSelected(roomId: String) {
        val room = currentState.rooms.firstOrNull { it.id == roomId } ?: return
        updateState {
            it.copy(
                rooms = it.rooms.map { currentRoom ->
                    if (currentRoom.id == roomId) {
                        currentRoom.copy(unreadCount = 0)
                    } else {
                        currentRoom
                    }
                },
                selectedRoomId = room.id,
                selectedRoomTitle = room.title,
                messages = roomMessages[room.id].orEmpty(),
                draftText = ""
            )
        }
    }

    fun onBackToRooms() {
        updateState {
            it.copy(
                selectedRoomId = null,
                selectedRoomTitle = null,
                messages = emptyList(),
                draftText = ""
            )
        }
    }

    fun onDraftChanged(text: String) {
        updateState { it.copy(draftText = text) }
    }

    fun onSendClicked() {
        val trimmedText = currentState.draftText.trim()
        val roomId = currentState.selectedRoomId
        if (trimmedText.isEmpty() || roomId == null) {
            return
        }

        val message = ChatMessage(
            id = "sent-${roomId}-${nextMessageId++}",
            roomId = roomId,
            senderName = "あなた",
            body = trimmedText,
            timeLabel = "今",
            isFromCurrentUser = true
        )
        val messages = roomMessages.getOrPut(roomId) { mutableListOf() }
        messages += message

        updateState {
            it.copy(
                rooms = it.rooms.map { room ->
                    if (room.id == roomId) {
                        room.copy(lastMessage = trimmedText, unreadCount = 0)
                    } else {
                        room
                    }
                },
                messages = messages.toList(),
                draftText = ""
            )
        }
    }

    private companion object {
        private var nextMessageId = 1

        private val initialRooms = listOf(
            ChatRoom(
                id = "operations",
                title = "運営本部",
                lastMessage = "巡回前に配置表を確認してください。",
                unreadCount = 2
            ),
            ChatRoom(
                id = "security",
                title = "警備チーム",
                lastMessage = "西ゲートの導線を調整しました。",
                unreadCount = 0
            ),
            ChatRoom(
                id = "reception",
                title = "受付チーム",
                lastMessage = "予備の名札を受付横に置きました。",
                unreadCount = 1
            )
        )

        private val initialMessages = listOf(
            ChatMessage(
                id = "operations-1",
                roomId = "operations",
                senderName = "運営本部",
                body = "巡回前に配置表を確認してください。",
                timeLabel = "09:10"
            ),
            ChatMessage(
                id = "operations-2",
                roomId = "operations",
                senderName = "運営本部",
                body = "変更点があればこのチャットで共有します。",
                timeLabel = "09:12"
            ),
            ChatMessage(
                id = "security-1",
                roomId = "security",
                senderName = "警備チーム",
                body = "西ゲートの導線を調整しました。",
                timeLabel = "08:55"
            ),
            ChatMessage(
                id = "reception-1",
                roomId = "reception",
                senderName = "受付チーム",
                body = "予備の名札を受付横に置きました。",
                timeLabel = "08:40"
            )
        )
    }
}
