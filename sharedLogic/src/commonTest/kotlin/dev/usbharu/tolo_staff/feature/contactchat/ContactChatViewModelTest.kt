package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    fun `initial load shows rooms from service`() = runTest {
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
    fun `initial load supports empty state`() = runTest {
        val viewModel = createViewModel(FakeContactChatService(), StandardTestDispatcher(testScheduler))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.rooms.isEmpty())
        assertNull(viewModel.uiState.value.selectedRoomId)

        viewModel.clear()
    }

    @Test
    fun `room selection loads messages`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            messagesByRoom = mutableMapOf(
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
            messagesByRoom = mutableMapOf("thread-1" to emptyList())
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
            messagesByRoom = mutableMapOf("thread-1" to emptyList()),
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
    fun `non simple payloads stay as system events`() = runTest {
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            messagesByRoom = mutableMapOf(
                "thread-1" to listOf(
                    ChatMessage(
                        id = "event-1",
                        roomId = "thread-1",
                        senderName = "system",
                        body = "指示が共有されました: inst-1",
                        isSystemEvent = true
                    )
                )
            )
        )

        val viewModel = createViewModel(service, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.single().isSystemEvent)

        viewModel.clear()
    }

    private fun createViewModel(
        service: FakeContactChatService,
        dispatcher: CoroutineContext
    ): ContactChatViewModel {
        return ContactChatViewModel(
            service = service,
            coroutineContext = dispatcher,
            pollIntervalMillis = 60_000L
        )
    }
}

private class FakeContactChatService(
    private var rooms: List<ChatRoom> = emptyList(),
    private val messagesByRoom: MutableMap<String, List<ChatMessage>> = mutableMapOf(),
    private val sendShouldFail: Boolean = false,
) : ContactChatService {
    var afterSendMessages: List<ChatMessage>? = null

    override suspend fun listRooms(currentStaffId: String): List<ChatRoom> = rooms

    override suspend fun listMessages(roomId: String, currentStaffId: String): List<ChatMessage> {
        return messagesByRoom[roomId].orEmpty()
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
        rooms = rooms.map { room ->
            if (room.id == roomId) room.copy(lastMessage = text) else room
        }
    }
}
