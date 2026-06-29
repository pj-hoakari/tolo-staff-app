package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.CurrentStaffMember
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
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
    fun `send waits for server message ids on success`() = runTest {
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
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(viewModel.uiState.value.isSending)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("server-1", viewModel.uiState.value.messages.single().id)
        assertEquals("配置につきました", viewModel.uiState.value.rooms.single().lastMessage)

        viewModel.clear()
    }

    @Test
    fun `send preserves empty message list on failure without local ids`() = runTest {
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
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher,
        )
    }

    @Test
    fun `current staff session drives room observation and send sender id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato")),
            initialMessagesByRoom = mutableMapOf("thread-1" to emptyList())
        )
        val session = createSession(dispatcher)
        val viewModel = ContactChatViewModel(
            service = service,
            currentStaffSession = session,
            coroutineContext = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(listOf("tanaka"), service.observedRoomStaffIds)

        session.selectStaff("sato")
        advanceUntilIdle()

        assertEquals(listOf("tanaka", "sato"), service.observedRoomStaffIds)

        viewModel.onRoomSelected("thread-1")
        advanceUntilIdle()
        viewModel.onDraftChanged("確認します")
        viewModel.onSendClicked()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        advanceUntilIdle()

        assertEquals("sato", service.lastSentStaffId)

        viewModel.clear()
    }

    @Test
    fun `unknown current staff does not observe rooms`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = FakeContactChatService(
            rooms = listOf(ChatRoom(id = "thread-1", title = "sato"))
        )
        val viewModel = ContactChatViewModel(
            service = service,
            currentStaffSession = MockCurrentStaffSession(coroutineContext = dispatcher),
            coroutineContext = dispatcher,
        )

        advanceUntilIdle()

        assertTrue(service.observedRoomStaffIds.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("スタッフ情報を取得できませんでした", viewModel.uiState.value.errorMessage)

        viewModel.clear()
    }

    private fun createSession(dispatcher: CoroutineContext): CurrentStaffSession =
        MockCurrentStaffSession(
            initialStaff = listOf(
                CurrentStaffMember("tanaka", "田中", "Aゲート担当"),
                CurrentStaffMember("sato", "佐藤", "巡回担当"),
            ),
            coroutineContext = dispatcher
        )
}

private class FakeContactChatService(
    rooms: List<ChatRoom> = emptyList(),
    initialMessagesByRoom: MutableMap<String, List<ChatMessage>> = mutableMapOf(),
    private val sendShouldFail: Boolean = false,
) : ContactChatService {
    val roomsFlow = MutableStateFlow(rooms)
    val messagesByRoom = initialMessagesByRoom
    private val messagesFlow = MutableStateFlow(initialMessagesByRoom.toMap())
    val observedRoomStaffIds = mutableListOf<String>()
    val observedMessageStaffIds = mutableListOf<String>()
    var lastSentStaffId: String? = null

    var afterSendMessages: List<ChatMessage>? = null

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> {
        observedRoomStaffIds += currentStaffId
        return roomsFlow
    }

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> {
        observedMessageStaffIds += currentStaffId
        return messagesFlow.map { it[roomId].orEmpty() }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        lastSentStaffId = currentStaffId
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
