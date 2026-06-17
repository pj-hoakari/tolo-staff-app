package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ContactChatViewModel(
    private val service: ContactChatService,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : StateEffectViewModel<ContactChatUiState, Unit>(
    initialState = ContactChatUiState(isLoading = true),
    coroutineContext = coroutineContext
) {
    private var roomsJob: Job? = null
    private var selectedRoomJob: Job? = null

    init {
        observeRooms()
    }

    fun onRoomSelected(roomId: String) {
        val room = currentState.rooms.firstOrNull { it.id == roomId } ?: return
        updateState {
            it.copy(
                selectedRoomId = room.id,
                selectedRoomTitle = room.title,
                messages = emptyList(),
                errorMessage = null,
                isLoading = true,
            )
        }
        observeSelectedRoom(room.id)
    }

    fun onBackToRooms() {
        updateState {
            it.copy(
                selectedRoomId = null,
                selectedRoomTitle = null,
                messages = emptyList(),
                draftText = "",
                errorMessage = null,
                isLoading = false,
            )
        }
        selectedRoomJob?.cancel()
    }

    fun onDraftChanged(text: String) {
        updateState { it.copy(draftText = text) }
    }

    fun onSendClicked() {
        val roomId = currentState.selectedRoomId ?: return
        val text = currentState.draftText.trim()
        if (text.isEmpty() || currentState.isSending) {
            return
        }

        val optimisticMessage = ChatMessage(
            id = "local-${nextLocalMessageId++}",
            roomId = roomId,
            senderName = CURRENT_STAFF_ID,
            body = text,
            timeLabel = null,
            isFromCurrentUser = true,
        )

        updateState {
            it.copy(
                messages = it.messages + optimisticMessage,
                draftText = "",
                isSending = true,
                errorMessage = null,
                rooms = it.rooms.map { room ->
                    if (room.id == roomId) room.copy(lastMessage = text) else room
                }
            )
        }

        viewModelScope.launch {
            runCatching {
                service.sendSimpleMessage(roomId, CURRENT_STAFF_ID, text)
                updateState { it.copy(isSending = false, errorMessage = null) }
            }.onFailure { throwable ->
                updateState { state ->
                    state.copy(
                        isSending = false,
                        errorMessage = throwable.message ?: "メッセージ送信に失敗しました",
                        messages = state.messages.filterNot { it.id == optimisticMessage.id },
                    )
                }
            }
        }
    }

    private fun observeRooms() {
        roomsJob?.cancel()
        roomsJob = service.observeRooms(CURRENT_STAFF_ID)
            .onEach { rooms ->
                val selectedRoomId = currentState.selectedRoomId?.takeIf { roomId ->
                    rooms.any { it.id == roomId }
                }
                updateState {
                    it.copy(
                        rooms = rooms,
                        selectedRoomId = selectedRoomId,
                        selectedRoomTitle = rooms.firstOrNull { room -> room.id == selectedRoomId }?.title,
                        messages = if (selectedRoomId == null) emptyList() else it.messages,
                        isLoading = selectedRoomId != null && it.messages.isEmpty(),
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
                if (selectedRoomId == null) {
                    selectedRoomJob?.cancel()
                    updateState { it.copy(messages = emptyList(), isLoading = false) }
                }
            }
            .catch { throwable ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "チャット一覧の購読に失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSelectedRoom(roomId: String) {
        selectedRoomJob?.cancel()
        selectedRoomJob = service.observeMessages(roomId, CURRENT_STAFF_ID)
            .onEach { messages ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSending = false,
                        errorMessage = null,
                        messages = messages,
                    )
                }
            }
            .catch { throwable ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSending = false,
                        errorMessage = throwable.message ?: "チャットの読み込みに失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun clear() {
        roomsJob?.cancel()
        selectedRoomJob?.cancel()
        super.clear()
    }

    private companion object {
        const val CURRENT_STAFF_ID = "tanaka"
        var nextLocalMessageId = 1L
    }
}
