package dev.usbharu.tolo_staff.feature.contactchat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactChatViewModelTest {
    @Test
    fun `initial state shows mock rooms`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        assertEquals(3, viewModel.uiState.value.rooms.size)
        assertEquals("運営本部", viewModel.uiState.value.rooms.first().title)
        assertNull(viewModel.uiState.value.selectedRoomId)

        viewModel.clear()
    }

    @Test
    fun `room selection shows room messages`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onRoomSelected("security")

        assertEquals("security", viewModel.uiState.value.selectedRoomId)
        assertEquals("警備チーム", viewModel.uiState.value.selectedRoomTitle)
        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.clear()
    }

    @Test
    fun `room selection clears unread count`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        assertEquals(2, viewModel.uiState.value.rooms.first { it.id == "operations" }.unreadCount)

        viewModel.onRoomSelected("operations")

        assertEquals(0, viewModel.uiState.value.rooms.first { it.id == "operations" }.unreadCount)

        viewModel.clear()
    }

    @Test
    fun `back to rooms clears selected room and draft`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onRoomSelected("operations")
        viewModel.onDraftChanged("確認しました")
        viewModel.onBackToRooms()

        assertNull(viewModel.uiState.value.selectedRoomId)
        assertEquals(emptyList(), viewModel.uiState.value.messages)
        assertEquals("", viewModel.uiState.value.draftText)

        viewModel.clear()
    }

    @Test
    fun `draft update changes draft text`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onDraftChanged("現地到着しました")

        assertEquals("現地到着しました", viewModel.uiState.value.draftText)

        viewModel.clear()
    }

    @Test
    fun `empty send is ignored`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onRoomSelected("operations")
        val messageCount = viewModel.uiState.value.messages.size
        viewModel.onDraftChanged("   ")
        viewModel.onSendClicked()

        assertEquals(messageCount, viewModel.uiState.value.messages.size)
        assertEquals("   ", viewModel.uiState.value.draftText)

        viewModel.clear()
    }

    @Test
    fun `send appends current user message and clears draft`() = runTest {
        val viewModel = ContactChatViewModel(UnconfinedTestDispatcher(testScheduler))

        viewModel.onRoomSelected("operations")
        val messageCount = viewModel.uiState.value.messages.size
        viewModel.onDraftChanged("配置につきました")
        viewModel.onSendClicked()

        val state = viewModel.uiState.value
        assertEquals(messageCount + 1, state.messages.size)
        assertEquals("", state.draftText)
        assertEquals("配置につきました", state.messages.last().body)
        assertTrue(state.messages.last().isFromCurrentUser)
        assertEquals("配置につきました", state.rooms.first { it.id == "operations" }.lastMessage)

        viewModel.clear()
    }
}
