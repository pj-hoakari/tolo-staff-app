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
import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus
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
        assertEquals(PlacementPhase.ACTIVE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertEquals("現在の配置", viewModel.uiState.value.homeOverview.placementStatus.headline)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction)
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(emptyList(), viewModel.uiState.value.instructionsTab.otherInstructions.map { it.id })
        assertEquals(3, viewModel.uiState.value.reportsTab.reportTypes.size)
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
    fun `pending assignment maps to pending change placement status`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate B",
                        placementDetail = "South entrance",
                    ),
                    currentPlacementName = "Gate B",
                    currentAssignmentId = "assign-pending",
                    currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.PENDING,
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals(PlacementPhase.PENDING_CHANGE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertEquals("配置が変更されました", viewModel.uiState.value.homeOverview.placementStatus.headline)
        assertEquals("確認しました", viewModel.uiState.value.homeOverview.placementStatus.buttonLabel)
        assertTrue(viewModel.uiState.value.homeOverview.placementStatus.showsActionButton)

        viewModel.clear()
    }

    @Test
    fun `en route assignment maps to moving placement status`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate C",
                    ),
                    currentPlacementName = "Gate C",
                    currentAssignmentId = "assign-route",
                    currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.EN_ROUTE,
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals(PlacementPhase.EN_ROUTE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertEquals("「Gate C」へ移動中", viewModel.uiState.value.homeOverview.placementStatus.headline)
        assertEquals("到着", viewModel.uiState.value.homeOverview.placementStatus.buttonLabel)
        assertTrue(viewModel.uiState.value.homeOverview.placementStatus.showsActionButton)

        viewModel.clear()
    }

    @Test
    fun `placement confirmation advances local state to en route then active`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate D",
                    ),
                    currentPlacementName = "Gate D",
                    currentAssignmentId = "assign-local",
                    currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.PENDING,
                )
            )
        )
        val assignmentStatusService = FakeAssignmentStatusService()
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            assignmentStatusService = assignmentStatusService,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onPlacementChangeConfirmed()
        assertEquals(PlacementPhase.EN_ROUTE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertEquals("到着", viewModel.uiState.value.homeOverview.placementStatus.buttonLabel)

        viewModel.onPlacementArrivalConfirmed()
        assertEquals(PlacementPhase.ACTIVE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertFalse(viewModel.uiState.value.homeOverview.placementStatus.showsActionButton)

        assertEquals(
            listOf(
                "assign-local" to OperationAssignmentStatus.EN_ROUTE,
                "assign-local" to OperationAssignmentStatus.ACTIVE,
            ),
            assignmentStatusService.updatedStatuses,
        )

        viewModel.clear()
    }

    @Test
    fun `placement status update failure surfaces error message`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate G",
                    ),
                    currentPlacementName = "Gate G",
                    currentAssignmentId = "assign-fail",
                    currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.PENDING,
                )
            )
        )
        val assignmentStatusService = FakeAssignmentStatusService(errorMessage = "network error")
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            assignmentStatusService = assignmentStatusService,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onPlacementChangeConfirmed()

        assertEquals(
            listOf("assign-fail" to OperationAssignmentStatus.EN_ROUTE),
            assignmentStatusService.updatedStatuses,
        )
        assertEquals("network error", viewModel.uiState.value.errorMessage)

        viewModel.clear()
    }

    @Test
    fun `new assignment clears local placement override`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        placementName = "Gate E",
                    ),
                    currentPlacementName = "Gate E",
                    currentAssignmentId = "assign-old",
                    currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.PENDING,
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onPlacementChangeConfirmed()
        assertEquals(PlacementPhase.EN_ROUTE, viewModel.uiState.value.homeOverview.placementStatus.phase)

        repository.emit(
            "tanaka",
            AppShellOperationsProjection(
                homeOverview = AppShellHomeOverview(
                    placementName = "Gate F",
                ),
                currentPlacementName = "Gate F",
                currentAssignmentId = "assign-new",
                currentAssignmentStatus = dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus.PENDING,
            )
        )

        assertEquals("assign-new", viewModel.uiState.value.homeOverview.placementStatus.assignId)
        assertEquals(PlacementPhase.PENDING_CHANGE, viewModel.uiState.value.homeOverview.placementStatus.phase)
        assertEquals("配置が変更されました", viewModel.uiState.value.homeOverview.placementStatus.headline)

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

        assertEquals("missing-id", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
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
    fun `opening instruction thread switches to contacts and selects thread`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            points = listOf(
                OperationPoint(
                    updatedAt = "",
                    reason = "test",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance",
                )
            ),
            instructions = listOf(
                OperationInstruction(
                    updatedAt = "",
                    reason = "test",
                    entityId = "instruction-gate-a",
                    instructionId = "instruction-gate-a",
                    threadId = "thread-gate-a",
                    title = "Shift update",
                    description = "Move barricades",
                    pointIds = listOf("gate-a"),
                    staffIds = listOf("tanaka"),
                    status = dev.usbharu.tolo_staff.streaming.OperationInstructionStatus.ACTIVE,
                )
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "thread-gate-a",
                    threadId = "thread-gate-a",
                    members = listOf("tanaka", "hq"),
                    displayTitle = "本部 / Gate A",
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "test",
                    entityId = "message-gate-a",
                    messageId = "message-gate-a",
                    threadId = "thread-gate-a",
                    instructionId = "instruction-gate-a",
                    staffId = "hq",
                    messageType = OperationMessageType.SIMPLE,
                    text = "南口の状況を共有してください",
                )
            )
        )
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        currentInstruction = "Move barricades",
                        currentInstructionId = "instruction-gate-a",
                    ),
                    currentPlacementName = "Gate A"
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onInstructionSelected("instruction-gate-a")
        viewModel.onInstructionThreadOpened()

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals("thread-gate-a", viewModel.uiState.value.contactsTab.selectedThread?.id)
        assertEquals("本部 / Gate A", viewModel.uiState.value.contactsTab.selectedThread?.title)
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

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread)
        viewModel.clear()
    }

    @Test
    fun `report flow opens submitted thread in contacts and adds it to thread list`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            points = listOf(
                OperationPoint(
                    updatedAt = "",
                    reason = "test",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(
                submittedReport = SubmittedReport(
                    reportId = "report-created-1",
                    threadId = "thread-created-1",
                )
            ),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onReportTypeSelected("queue")
        viewModel.onReportCommentChanged("最後尾が歩道へ伸びています")
        viewModel.onReportUrgencySelected("高")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportPlaceSelected("gate-a")
        viewModel.onReportSubmitted()

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals(ReportFlowStep.TYPE_SELECTION, viewModel.uiState.value.reportsTab.step)
        assertEquals(null, viewModel.uiState.value.reportsTab.draft.selectedPlaceName)
        assertEquals("導線報告 / Gate A", viewModel.uiState.value.contactsTab.selectedThread?.title)
        assertTrue(viewModel.uiState.value.contactsTab.selectedThread?.messages.orEmpty().isEmpty())
        assertEquals("report-created-1", viewModel.uiState.value.reportsTab.relatedReports.first().reportId)
        assertTrue(viewModel.uiState.value.contactsTab.threads.any { it.id == "thread-created-1" })
        assertEquals("thread-created-1", viewModel.uiState.value.contactsTab.selectedThread?.id)
        viewModel.clear()
    }

    @Test
    fun `submitted report thread switches from local placeholder to remote thread details when polling updates arrive`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            points = listOf(
                OperationPoint(
                    updatedAt = "",
                    reason = "test",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(
                reports = listOf(
                    RelevantReport(
                        reportId = "report-created-1",
                        threadId = "thread-created-1",
                        authorStaffId = "tanaka",
                        title = "導線報告",
                        summary = "最後尾が歩道へ伸びています [高]",
                        priorityLabel = "高",
                        createdAtLabel = "2026-06-19T09:00:00Z",
                    )
                ),
                submittedReport = SubmittedReport(
                    reportId = "report-created-1",
                    threadId = "thread-created-1",
                )
            ),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onReportTypeSelected("queue")
        viewModel.onReportCommentChanged("最後尾が歩道へ伸びています")
        viewModel.onReportUrgencySelected("高")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportPlaceSelected("gate-a")
        viewModel.onReportSubmitted()

        assertTrue(viewModel.uiState.value.contactsTab.selectedThread?.messages.orEmpty().isEmpty())

        dataSource.threadsFlow.value = listOf(
            OperationThread(
                updatedAt = "2026-06-19T09:00:00Z",
                reason = "test",
                entityId = "thread-created-1",
                threadId = "thread-created-1",
                members = listOf("tanaka", "hq"),
                displayTitle = "本部 / Gate A",
            )
        )
        dataSource.messagesFlow.value = listOf(
            OperationMessage(
                updatedAt = "2026-06-19T09:00:00Z",
                reason = "test",
                entityId = "message-created-1",
                messageId = "message-created-1",
                threadId = "thread-created-1",
                staffId = "tanaka",
                messageType = OperationMessageType.REPORT,
                reportId = "report-created-1",
            )
        )

        assertEquals("本部 / Gate A", viewModel.uiState.value.contactsTab.selectedThread?.title)
        assertEquals("report-created-1", viewModel.uiState.value.contactsTab.selectedThread?.messages?.singleOrNull()?.reportId)
        assertEquals("導線報告", viewModel.uiState.value.contactsTab.selectedThread?.messages?.singleOrNull()?.reportTitle)
        assertEquals(
            "報告が共有されました: report-created-1",
            viewModel.uiState.value.contactsTab.threads.firstOrNull { it.id == "thread-created-1" }?.lastMessagePreview
        )

        viewModel.clear()
    }

    @Test
    fun `report submission failure keeps draft state and surfaces error`() = runTest {
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
                )
            ),
            points = listOf(
                OperationPoint(
                    updatedAt = "",
                    reason = "test",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(submitErrorMessage = "submit failed"),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onReportTypeSelected("queue")
        viewModel.onReportCommentChanged("最後尾が歩道へ伸びています")
        viewModel.onReportUrgencySelected("高")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportPlaceSelected("gate-a")
        viewModel.onReportSubmitted()

        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(ReportFlowStep.PLACE_SELECTION, viewModel.uiState.value.reportsTab.step)
        assertEquals("gate-a", viewModel.uiState.value.reportsTab.draft.selectedPlaceId)
        assertEquals("submit failed", viewModel.uiState.value.reportsTab.reportsErrorMessage)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread)
        viewModel.clear()
    }

    @Test
    fun `related reports load into detail and can open thread in contacts`() = runTest {
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
                )
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "report-thread-1",
                    threadId = "report-thread-1",
                    members = listOf("tanaka", "hq"),
                    displayTitle = "本部 / 南口",
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "test",
                    entityId = "report-message-1",
                    messageId = "report-message-1",
                    threadId = "report-thread-1",
                    staffId = "tanaka",
                    messageType = OperationMessageType.REPORT,
                    reportId = "report-1",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(
                reports = listOf(
                    RelevantReport(
                        reportId = "report-1",
                        threadId = "report-thread-1",
                        authorStaffId = "tanaka",
                        title = "導線報告",
                        summary = "南口の入場列は安定しています",
                        priorityLabel = "通常",
                        createdAtLabel = "2026-06-19T09:00:00Z",
                    )
                )
            ),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals(1, viewModel.uiState.value.reportsTab.relatedReports.size)

        viewModel.onReportSelected("report-1")

        assertEquals(AppTab.REPORTS, viewModel.uiState.value.selectedTab)
        assertEquals("導線報告", viewModel.uiState.value.reportsTab.selectedReport?.title)
        assertEquals("南口の入場列は安定しています", viewModel.uiState.value.reportsTab.selectedReport?.summary)

        viewModel.onReportThreadOpened()

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals("本部 / 南口", viewModel.uiState.value.contactsTab.selectedThread?.title)
        assertEquals("report-1", viewModel.uiState.value.contactsTab.selectedThread?.messages?.singleOrNull()?.reportId)
        assertEquals("導線報告", viewModel.uiState.value.contactsTab.selectedThread?.messages?.singleOrNull()?.reportTitle)
        viewModel.onContactBackToList()
        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread)
        assertEquals("導線報告", viewModel.uiState.value.reportsTab.selectedReport?.title)
        viewModel.onReportDetailClosed()
        assertEquals(null, viewModel.uiState.value.reportsTab.selectedReport)
        viewModel.clear()
    }

    @Test
    fun `contact report message opens report detail and closing detail returns to same thread`() = runTest {
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
                )
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "report-thread-1",
                    threadId = "report-thread-1",
                    members = listOf("tanaka", "hq"),
                    displayTitle = "本部 / 南口",
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "test",
                    entityId = "report-message-1",
                    messageId = "report-message-1",
                    threadId = "report-thread-1",
                    staffId = "tanaka",
                    messageType = OperationMessageType.REPORT,
                    reportId = "report-1",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(
                reports = listOf(
                    RelevantReport(
                        reportId = "report-1",
                        threadId = "report-thread-1",
                        authorStaffId = "tanaka",
                        title = "導線報告",
                        summary = "南口の入場列は安定しています",
                        priorityLabel = "通常",
                        createdAtLabel = "2026-06-19T09:00:00Z",
                    )
                )
            ),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("report-thread-1")
        viewModel.onContactReportMessageSelected("report-1")

        assertEquals(AppTab.REPORTS, viewModel.uiState.value.selectedTab)
        assertEquals("導線報告", viewModel.uiState.value.reportsTab.selectedReport?.title)
        assertEquals("report-thread-1", viewModel.uiState.value.reportsTab.openedFromContactThreadId)

        viewModel.onReportDetailClosed()

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals("report-thread-1", viewModel.uiState.value.contactsTab.selectedThread?.id)
        assertEquals(null, viewModel.uiState.value.reportsTab.selectedReport)
        assertEquals(null, viewModel.uiState.value.reportsTab.openedFromContactThreadId)
        viewModel.clear()
    }

    @Test
    fun `report messages without related report fall back to plain message rendering data`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            staff = listOf(
                OperationStaff(
                    updatedAt = "",
                    reason = "test",
                    entityId = "tanaka",
                    staffId = "tanaka",
                    name = "田中",
                )
            ),
            threads = listOf(
                OperationThread(
                    updatedAt = "",
                    reason = "test",
                    entityId = "report-thread-missing",
                    threadId = "report-thread-missing",
                    members = listOf("tanaka", "hq"),
                )
            ),
            messages = listOf(
                OperationMessage(
                    updatedAt = "2026-06-19T09:00:00Z",
                    reason = "test",
                    entityId = "report-message-missing",
                    messageId = "report-message-missing",
                    threadId = "report-thread-missing",
                    staffId = "tanaka",
                    messageType = OperationMessageType.REPORT,
                    reportId = "missing-report",
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            dataSource = dataSource,
            reportRepository = FakeReportRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onContactThreadSelected("report-thread-missing")

        val message = viewModel.uiState.value.contactsTab.selectedThread?.messages?.singleOrNull()
        assertEquals("missing-report", message?.reportId)
        assertEquals(null, message?.reportTitle)
        assertEquals("報告が共有されました: missing-report", message?.body)
        viewModel.clear()
    }

    @Test
    fun `related report loading failure updates error state`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            reportRepository = FakeReportRepository(errorMessage = "load failed"),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        assertEquals("load failed", viewModel.uiState.value.reportsTab.reportsErrorMessage)
        assertFalse(viewModel.uiState.value.reportsTab.isLoadingReports)
        viewModel.clear()
    }

    @Test
    fun `instruction status update is reflected in selected and featured instruction`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeOperationsStreamDataSource(
            points = listOf(
                OperationPoint(
                    updatedAt = "",
                    reason = "test",
                    entityId = "gate-a",
                    pointId = "gate-a",
                    name = "Gate A",
                    description = "North entrance",
                )
            ),
            instructions = listOf(
                OperationInstruction(
                    updatedAt = "",
                    reason = "test",
                    entityId = "instruction-gate-a",
                    instructionId = "instruction-gate-a",
                    title = "Shift update",
                    description = "Move barricades",
                    pointIds = listOf("gate-a"),
                    staffIds = listOf("tanaka"),
                    status = dev.usbharu.tolo_staff.streaming.OperationInstructionStatus.ACTIVE,
                )
            )
        )
        val repository = FakeOperationsOverviewRepository(
            projections = mapOf(
                "tanaka" to AppShellOperationsProjection(
                    homeOverview = AppShellHomeOverview(
                        currentInstruction = "Move barricades",
                        currentInstructionId = "instruction-gate-a",
                    ),
                    currentPlacementName = "Gate A"
                )
            )
        )
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            dataSource = dataSource,
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onInstructionSelected("instruction-gate-a")
        viewModel.onInstructionStatusUpdated(InstructionProgressStatus.COMPLETED)

        assertEquals("完了", viewModel.uiState.value.instructionsTab.selectedInstruction?.statusLabel)
        assertEquals("完了", viewModel.uiState.value.instructionsTab.featuredInstruction?.statusLabel)
        assertEquals("完了", viewModel.uiState.value.homeOverview.currentInstructionStatusLabel)
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
        assertEquals("home-overview-instruction", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
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
    fun `sender name alone does not restore current user when streamed sender id is null literal`() = runTest {
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
                    entityId = "message-1",
                    messageId = "message-1",
                    threadId = "thread-gate-a",
                    staffId = "null",
                    messageType = OperationMessageType.SIMPLE,
                    text = "こちら配置済みです",
                    senderName = "田中",
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
        assertEquals(false, message?.isCurrentUser)

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
    private val projectionFlows = projections.mapValues { MutableStateFlow(it.value) }.toMutableMap()

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> {
        observedStaffIds += currentStaffId
        return projectionFlows.getOrPut(currentStaffId) {
            MutableStateFlow(projections[currentStaffId] ?: projections.getValue("tanaka"))
        }
    }

    fun emit(currentStaffId: String, projection: AppShellOperationsProjection) {
        projectionFlows.getOrPut(currentStaffId) {
            MutableStateFlow(projection)
        }.value = projection
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

private class FakeReportRepository(
    private val reports: List<RelevantReport> = emptyList(),
    private val errorMessage: String? = null,
    private val submittedReport: SubmittedReport = SubmittedReport(
        reportId = "report-1",
        threadId = "thread-1",
    ),
    private val submitErrorMessage: String? = null,
) : ReportRepository {
    override suspend fun listRelevantReports(currentStaffId: String): List<RelevantReport> {
        errorMessage?.let { error(it) }
        return reports
    }

    override suspend fun submitReport(
        currentStaffId: String,
        title: String,
        summary: String,
        priorityLabel: String,
    ): SubmittedReport {
        submitErrorMessage?.let { error(it) }
        return submittedReport
    }
}

private class FakeAssignmentStatusService(
    private val errorMessage: String? = null,
) : AssignmentStatusService {
    val updatedStatuses = mutableListOf<Pair<String, OperationAssignmentStatus>>()

    override suspend fun updateStatus(assignId: String, status: OperationAssignmentStatus) {
        updatedStatuses += assignId to status
        errorMessage?.let { error(it) }
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
    val threadsFlow = MutableStateFlow(threads)
    val messagesFlow = MutableStateFlow(messages)

    override fun observePoints(): Flow<List<OperationPoint>> = pointsFlow

    override fun observeStaff(): Flow<List<OperationStaff>> = staffFlow

    override fun observeAssignments(): Flow<List<OperationAssignment>> = assignmentsFlow

    override fun observeInstructions(): Flow<List<OperationInstruction>> = instructionsFlow

    override fun observeThreads(): Flow<List<OperationThread>> = threadsFlow

    override fun observeMessages(): Flow<List<OperationMessage>> = messagesFlow

    override fun start() = Unit

    override fun stop() = Unit
}
