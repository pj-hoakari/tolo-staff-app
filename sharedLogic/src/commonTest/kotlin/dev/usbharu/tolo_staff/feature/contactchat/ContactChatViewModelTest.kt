package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactChatViewModelTest {
    @Test
    fun `initial load shows streamed rooms from service`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato", lastMessage = "hello"))
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.rooms.size)
        assertEquals("thread-1", viewModel.uiState.value.rooms.single().id)

        viewModel.clear()
    }

    @Test
    fun `room selection subscribes to room messages`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            initialMessagesByRoom = mutableMapOf(
                "thread-1" to listOf(
                    ChatMessage(
                        id = "m-1",
                        roomId = "thread-1",
                        senderName = "sato",
                        body = "確認しました"
                    )
                )
            )
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()

        assertEquals("thread-1", viewModel.uiState.value.selectedRoomId)
        assertEquals("sato", viewModel.uiState.value.selectedRoomTitle)
        assertEquals(1, viewModel.uiState.value.messages.size)

        service.messagesByRoom["thread-1"] = listOf(
            ChatMessage(
                id = "m-2",
                roomId = "thread-1",
                senderName = "sato",
                body = "更新されました"
            )
        )
        service.emitMessages()
        advanceUntilIdle()

        assertEquals("m-2", viewModel.uiState.value.messages.single().id)
        viewModel.clear()
    }

    @Test
    fun `back to rooms clears selection and draft`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato"))
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()
        viewModel.onDraftChanged("確認しました")
        viewModel.onBackToRooms()

        assertNull(viewModel.uiState.value.selectedRoomId)
        assertEquals("", viewModel.uiState.value.draftText)
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.clear()
    }

    @Test
    fun `send adds optimistic message then reconciles on success`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            initialMessagesByRoom = mutableMapOf("thread-1" to emptyList())
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()

        service.afterSendMessages = listOf(
            ChatMessage(
                id = "server-1",
                roomId = "thread-1",
                senderName = "tanaka",
                body = "配置につきました",
                isFromCurrentUser = true
            )
        )

        viewModel.onDraftChanged("配置につきました")
        viewModel.onSendClicked()

        assertEquals("", viewModel.uiState.value.draftText)
        assertTrue(viewModel.uiState.value.messages.any { it.body == "配置につきました" })

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("server-1", viewModel.uiState.value.messages.single().id)
        assertEquals("配置につきました", viewModel.uiState.value.rooms.single().lastMessage)

        viewModel.clear()
    }

    @Test
    fun `send rolls back optimistic message on failure`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            initialMessagesByRoom = mutableMapOf("thread-1" to emptyList()),
            sendShouldFail = true
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()
        viewModel.onDraftChanged("送信失敗テスト")
        viewModel.onSendClicked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("メッセージ送信に失敗しました", viewModel.uiState.value.errorMessage)

        viewModel.clear()
    }

    @Test
    fun `room stream removal clears selected room`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            initialMessagesByRoom = mutableMapOf("thread-1" to emptyList())
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()

        service.roomsFlow.value = emptyList()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedRoomId)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        viewModel.clear()
    }

    private fun createViewModel(
        service: FakeContactChatService,
        dispatcher: CoroutineContext
    ): ContactChatViewModel {
        return ContactChatViewModel(
            service = service,
            coroutineContext = dispatcher,
        )
    }
}

private class FakeContactChatService(
    rooms: List<ChatRoom> = emptyList(),
    initialMessagesByRoom: MutableMap<String, List<ChatMessage>> = mutableMapOf(),
    private val sendShouldFail: Boolean = false,
) : ContactChatService {
    val roomsFlow = MutableStateFlow(rooms)
    val messagesByRoom = initialMessagesByRoom
    private val messagesFlow = MutableStateFlow(initialMessagesByRoom.toMap())

    var afterSendMessages: List<ChatMessage>? = null

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> = roomsFlow

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> {
        return messagesFlow.map { it[roomId].orEmpty() }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        if (sendShouldFail) {
            throw IllegalStateException("メッセージ送信に失敗しました")
        }

        val serverMessages = afterSendMessages ?: listOf(
            ChatMessage(
                id = "server-${text.length}",
                roomId = roomId,
                senderName = currentStaffId,
                body = text,
                isFromCurrentUser = true
            )
        )
        messagesByRoom[roomId] = serverMessages
        messagesFlow.value = messagesByRoom.toMap()
        roomsFlow.value = roomsFlow.value.map { room ->
            if (room.id == roomId) room.copy(lastMessage = text) else room
        }
    }

    fun emitMessages() {
        messagesFlow.value = messagesByRoom.toMap()
    }
}
