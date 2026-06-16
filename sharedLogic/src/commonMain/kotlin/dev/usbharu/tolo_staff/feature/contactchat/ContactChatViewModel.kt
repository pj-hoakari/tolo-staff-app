package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ContactChatViewModel(
    private val service: ContactChatService = GrpcContactChatService(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
    private val pollIntervalMillis: Long = 5_000L,
) : StateEffectViewModel<ContactChatUiState, Unit>(
    initialState = ContactChatUiState(isLoading = true),
    coroutineContext = coroutineContext
) {
    private var pollJob: Job? = null

    init {
        loadInitialState()
        startPolling()
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
        refreshSelectedRoom()
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
                refreshAll(isRefreshing = false)
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

    private fun loadInitialState() {
        viewModelScope.launch {
            refreshAll(showLoading = true)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(pollIntervalMillis)
                refreshAll(isRefreshing = true)
            }
        }
    }

    private fun refreshSelectedRoom() {
        val roomId = currentState.selectedRoomId ?: return
        viewModelScope.launch {
            runCatching {
                val messages = service.listMessages(roomId, CURRENT_STAFF_ID)
                updateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSending = false,
                        errorMessage = null,
                        messages = messages,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSending = false,
                        errorMessage = throwable.message ?: "チャットの読み込みに失敗しました",
                    )
                }
            }
        }
    }

    private suspend fun refreshAll(
        showLoading: Boolean = false,
        isRefreshing: Boolean = false,
    ) {
        updateState {
            it.copy(
                isLoading = showLoading,
                isRefreshing = isRefreshing,
                errorMessage = null,
            )
        }

        runCatching {
            val rooms = service.listRooms(CURRENT_STAFF_ID)
            val selectedRoomId = currentState.selectedRoomId?.takeIf { roomId ->
                rooms.any { it.id == roomId }
            }
            val messages = if (selectedRoomId != null) {
                service.listMessages(selectedRoomId, CURRENT_STAFF_ID)
            } else {
                emptyList()
            }

            updateState {
                it.copy(
                    rooms = rooms,
                    selectedRoomId = selectedRoomId,
                    selectedRoomTitle = rooms.firstOrNull { room -> room.id == selectedRoomId }?.title,
                    messages = messages,
                    isLoading = false,
                    isRefreshing = false,
                    isSending = false,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            updateState {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isSending = false,
                    errorMessage = throwable.message ?: "チャットの更新に失敗しました",
                )
            }
        }
    }

    override fun clear() {
        pollJob?.cancel()
        super.clear()
    }

    private companion object {
        const val CURRENT_STAFF_ID = "tanaka"
        var nextLocalMessageId = 1L
    }
}
