package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.feature.contactchat.ChatMessage
import dev.usbharu.tolo_staff.feature.contactchat.ChatRoom
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.streaming.AppShellOperationsProjection
import dev.usbharu.tolo_staff.streaming.CurrentStaffMember
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.OperationAssignment
import dev.usbharu.tolo_staff.streaming.OperationInstruction
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @Test
    fun `initial state reflects streamed overview`() = runTest {
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        eventName = "Tolo Staff Demo 2026",
                        eventTime = "Firestore Streaming Demo",
                        placementName = "Gate A",
                        placementDetail = "North entrance",
                        currentInstruction = "Shift update: Move barricades",
                        currentInstructionId = "instruction-gate-a",
                    ),
                    currentPlacementName = "Gate A"
                )
            )
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals("田中", viewModel.uiState.value.currentStaff.displayName)
        assertEquals("Gate A", viewModel.uiState.value.currentPlacementName)
        assertEquals("Tolo Staff Demo 2026", viewModel.uiState.value.homeOverview.eventName)
        assertEquals("North entrance", viewModel.uiState.value.homeOverview.placementDetail)
        assertEquals("Shift update: Move barricades", viewModel.uiState.value.homeOverview.currentInstruction)
        assertEquals("instruction-gate-a", viewModel.uiState.value.homeOverview.currentInstructionId)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction)
        assertEquals(null, viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(emptyList(), viewModel.uiState.value.instructionsTab.otherInstructions.map { it.id })
        assertEquals(0, viewModel.uiState.value.reportsTab.reportTypes.size)
        assertEquals(0, viewModel.uiState.value.contactsTab.threads.size)
        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(false, viewModel.uiState.value.isLoading)
        viewModel.clear()
    }

    @Test
    fun `tab selection updates selected tab`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        listOf(
            AppTab.INSTRUCTIONS,
            AppTab.REPORTS,
            AppTab.CONTACTS,
            AppTab.HOME
        ).forEach { tab ->
            viewModel.onTabSelected(tab)
            assertEquals(tab, viewModel.uiState.value.selectedTab)
        }

        viewModel.clear()
    }

    @Test
    fun `home instruction shortcut is ignored when there is no instruction detail loaded`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onHomeInstructionSelected()

        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals(null, viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(false, viewModel.uiState.value.instructionsTab.isShowingThread)
        viewModel.clear()
    }

    @Test
    fun `featured instruction falls back to first item when home overview id is missing`() = runTest {
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        currentInstruction = "No matching id",
                        currentInstructionId = "missing-id",
                    ),
                    currentPlacementName = "未配属"
                )
            )
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals(null, viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(emptyList(), viewModel.uiState.value.instructionsTab.otherInstructions.map { it.id })

        viewModel.clear()
    }

    @Test
    fun `opening instruction thread is ignored when there is no selected instruction`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onInstructionThreadOpened()

        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread?.id)
        assertEquals(false, viewModel.uiState.value.instructionsTab.isShowingThread)
        viewModel.clear()
    }

    @Test
    fun `closing contact thread keeps selection empty when no instruction thread was opened`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactBackToList()

        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread)
        viewModel.clear()
    }

    @Test
    fun `report flow stays inactive without dynamic report metadata`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onReportTypeSelected("queue")
        viewModel.onReportCommentChanged("最後尾が歩道へ伸びています")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportSubmitted()

        assertEquals(ReportFlowStep.TYPE_SELECTION, viewModel.uiState.value.reportsTab.step)
        assertEquals(null, viewModel.uiState.value.reportsTab.draft.selectedPlaceName)
        assertEquals(null, viewModel.uiState.value.reportsTab.submittedThread)
        viewModel.clear()
    }

    @Test
    fun `contact flow does not create local draft thread without backend support`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactNewThreadStarted()
        viewModel.onContactTargetTypeSelected(ContactTargetType.USER)
        viewModel.onContactTargetSelected("user-sato")
        viewModel.onContactDraftChanged("今どこにいますか")
        viewModel.onContactSendClicked()

        assertEquals(false, viewModel.uiState.value.contactsTab.isChoosingTargetType)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread?.title)
        assertEquals(0, viewModel.uiState.value.contactsTab.threads.size)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread?.messages?.lastOrNull()?.body)
        viewModel.clear()
    }

    @Test
    fun `contact thread send delegates to chat service and clears draft`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val chatService = FakeContactChatService()
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "田中",
                    roles = listOf("Aゲート担当"),
                ),
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "sato",
                    staffId = "sato",
                    name = "佐藤",
                    roles = listOf("巡回担当"),
                ),
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-1",
                    threadId = "thread-1",
                    members = listOf("tanaka", "sato"),
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "",
                    reason = "test",
                    entityId = "message-1",
                    messageId = "message-1",
                    threadId = "thread-1",
                    staffId = "sato",
                    messageType = OperationMessageType.SIMPLE,
                    text = "了解です",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            contactChatService = chatService,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("thread-1")
        viewModel.onContactDraftChanged("こちら配置済みです")
        viewModel.onContactSendClicked()

        assertEquals("", viewModel.uiState.value.contactsTab.selectedThread?.draftMessage)
        assertEquals(listOf(Triple("thread-1", "tanaka", "こちら配置済みです")), chatService.sentMessages)

        viewModel.clear()
    }

    @Test
    fun `contact thread draft survives overview refresh`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "田中",
                    roles = listOf("Aゲート担当"),
                ),
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "sato",
                    staffId = "sato",
                    name = "佐藤",
                    roles = listOf("巡回担当"),
                ),
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-1",
                    threadId = "thread-1",
                    members = listOf("tanaka", "sato"),
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "",
                    reason = "test",
                    entityId = "message-1",
                    messageId = "message-1",
                    threadId = "thread-1",
                    staffId = "sato",
                    messageType = OperationMessageType.SIMPLE,
                    text = "了解です",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("thread-1")
        viewModel.onContactDraftChanged("下書き")
        dataSource.messagesFlow.value = dataSource.messagesFlow.value + OperationMessage(
            updatedAt = "",
            reason = "test",
            entityId = "message-2",
            messageId = "message-2",
            threadId = "thread-1",
            staffId = "tanaka",
            messageType = OperationMessageType.SIMPLE,
            text = "追加メッセージ",
        )

        assertEquals("下書き", viewModel.uiState.value.contactsTab.selectedThread?.draftMessage)
        assertTrue(viewModel.uiState.value.contactsTab.selectedThread?.canReply == true)

        viewModel.clear()
    }

    @Test
    fun `switching current staff refreshes overview and current user labels`() = runTest {
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate A",
                        placementDetail = "North entrance",
                        currentInstruction = "Tanaka instruction",
                    ),
                    currentPlacementName = "Gate A"
                ),
                "sato" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Patrol",
                        placementDetail = "West hall",
                        currentInstruction = "Sato instruction",
                    ),
                    currentPlacementName = "Patrol"
                ),
            )
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onCurrentStaffSelected("sato")

        assertEquals(listOf("tanaka", "sato"), repository.observedStaffIds)
        assertEquals("佐藤", viewModel.uiState.value.currentStaff.displayName)
        assertEquals("Patrol", viewModel.uiState.value.currentPlacementName)
        assertEquals("Sato instruction", viewModel.uiState.value.homeOverview.currentInstruction)
        assertEquals(null, viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction)

        viewModel.clear()
    }

    @Test
    fun `contact thread messages mark sender and current user correctly`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "田中",
                    roles = listOf("Aゲート担当"),
                ),
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "sato",
                    staffId = "sato",
                    name = "佐藤",
                    roles = listOf("巡回担当"),
                ),
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-1",
                    threadId = "thread-1",
                    members = listOf("tanaka", "sato"),
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-18T09:00:00Z",
                    reason = "test",
                    entityId = "message-1",
                    messageId = "message-1",
                    threadId = "thread-1",
                    staffId = "sato",
                    messageType = OperationMessageType.SIMPLE,
                    text = "了解です",
                ),
                OperationMessage(
                    updatedAt = "2026-06-18T09:01:00Z",
                    reason = "test",
                    entityId = "message-2",
                    messageId = "message-2",
                    threadId = "thread-1",
                    staffId = "tanaka",
                    messageType = OperationMessageType.SIMPLE,
                    text = "こちら配置済みです",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("thread-1")

        val initialMessages = viewModel.uiState.value.contactsTab.selectedThread?.messages.orEmpty()
        assertEquals(listOf("佐藤", "田中"), initialMessages.map { it.senderName })
        assertEquals(listOf(false, true), initialMessages.map { it.isCurrentUser })

        viewModel.onCurrentStaffSelected("sato")
        viewModel.onContactThreadSelected("thread-1")

        val switchedMessages = viewModel.uiState.value.contactsTab.selectedThread?.messages.orEmpty()
        assertEquals(listOf("佐藤", "田中"), switchedMessages.map { it.senderName })
        assertEquals(listOf(true, false), switchedMessages.map { it.isCurrentUser })
        assertFalse(switchedMessages.any { it.senderName == "null" })

        viewModel.clear()
    }

    @Test
    fun `client message id restores sender when streamed sender is null literal`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "田中",
                    roles = listOf("Aゲート担当"),
                ),
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "sato",
                    staffId = "sato",
                    name = "佐藤",
                    roles = listOf("巡回担当"),
                ),
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-gate-a",
                    threadId = "thread-gate-a",
                    members = listOf("tanaka", "sato"),
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "message.created",
                    entityId = "client-tanaka-12345",
                    messageId = "client-tanaka-12345",
                    threadId = "thread-gate-a",
                    staffId = "null",
                    messageType = OperationMessageType.SIMPLE,
                    text = "こちら配置済みです",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("thread-gate-a")

        val message = viewModel.uiState.value.contactsTab.selectedThread?.messages?.single()
        assertEquals("田中", message?.senderName)
        assertEquals(true, message?.isCurrentUser)

        viewModel.clear()
    }

    @Test
    fun `unknown current staff does not start overview observation`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository()
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = MockCurrentStaffSession(coroutineContext = dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals(emptyList(), repository.observedStaffIds)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("スタッフ情報を取得できませんでした", viewModel.uiState.value.errorMessage)

        viewModel.clear()
    }

    @Test
    fun `contacts tab sorts threads by latest message and uses display title`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(updatedAt = "", reason = "test", entityId = "tanaka", staffId = "tanaka", name = "田中"),
                OperationStaff(updatedAt = "", reason = "test", entityId = "sato", staffId = "sato", name = "佐藤"),
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-old",
                    threadId = "thread-old",
                    members = listOf("tanaka", "sato"),
                ),
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-none",
                    threadId = "thread-none",
                    members = listOf("tanaka", "sato"),
                ),
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-new",
                    threadId = "thread-new",
                    members = listOf("tanaka", "sato"),
                    displayTitle = "Aゲート連絡",
                ),
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "test",
                    entityId = "message-1",
                    messageId = "message-1",
                    threadId = "thread-old",
                    staffId = "sato",
                    messageType = OperationMessageType.SIMPLE,
                    text = "old",
                ),
                OperationMessage(
                    updatedAt = "2026-06-19T10:00:00Z",
                    reason = "test",
                    entityId = "message-2",
                    messageId = "message-2",
                    threadId = "thread-new",
                    staffId = "sato",
                    messageType = OperationMessageType.SIMPLE,
                    text = "new",
                ),
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        val threads = viewModel.uiState.value.contactsTab.threads
        assertEquals(listOf("thread-new", "thread-old", "thread-none"), threads.map { it.id })
        assertEquals(listOf("Aゲート連絡", "佐藤", "佐藤"), threads.map { it.title })

        viewModel.onContactThreadSelected("thread-new")
        assertEquals("Aゲート連絡", viewModel.uiState.value.contactsTab.selectedThread?.title)

        viewModel.clear()
    }

    private fun createSession(dispatcher: CoroutineContext): CurrentStaffSession = MockCurrentStaffSession(
        initialStaff = listOf(
            CurrentStaffMember("tanaka", "田中", "Aゲート担当"),
            CurrentStaffMember("sato", "佐藤", "巡回担当"),
        ),
        coroutineContext = dispatcher
    )
}

private class FakeOperationsOverviewRepository(
    private val projections: Map<String, AppShellOperationsProjection> = mapOf(
        "tanaka" to AppShellOperationsProjection(
            homeOverview = AppShellHomeOverview(),
            currentPlacementName = "未配属"
        )
    )
) : OperationsOverviewRepository {
    val observedStaffIds = mutableListOf<String>()

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        observedStaffIds += currentStaffId
        return flowOf(
            projections[currentStaffId] ?: projections.getValue("tanaka")
        )
    }
}

private class FakeContactChatService : ContactChatService {
    val sentMessages = mutableListOf<Triple<String, String, String>>()

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> = flowOf(emptyList())

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> = flowOf(emptyList())

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        sentMessages += Triple(roomId, currentStaffId, text)
    }
}

private class FakeOperationsStreamDataSource(
    points: List<OperationPoint> = emptyList(),
    staff: List<OperationStaff> = emptyList(),
    assignments: List<OperationAssignment> = emptyList(),
    instructions: List<OperationInstruction> = emptyList(),
    threads: List<OperationThread> = emptyList(),
    messages: List<OperationMessage> = emptyList(),
) : OperationsStreamDataSource {
    private val pointsFlow = MutableStateFlow(points)
    private val staffFlow = MutableStateFlow(staff)
    private val assignmentsFlow = MutableStateFlow(assignments)
    private val instructionsFlow = MutableStateFlow(instructions)
    private val threadsStateFlow = MutableStateFlow(threads)
    val messagesFlow = MutableStateFlow(messages)

    override fun observePoints(): Flow<List<OperationPoint>> = pointsFlow

    override fun observeStaff(): Flow<List<OperationStaff>> = staffFlow

    override fun observeAssignments(): Flow<List<OperationAssignment>> = assignmentsFlow

    override fun observeInstructions(): Flow<List<OperationInstruction>> = instructionsFlow

    override fun observeThreads(): Flow<List<OperationThread>> = threadsStateFlow

    override fun observeMessages(): Flow<List<OperationMessage>> = messagesFlow

    override fun start() = Unit

    override fun stop() = Unit
}
