package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.AppShellOperationsProjection
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.defaultMockStaffMembers
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(listOf("instruction-patrol"), viewModel.uiState.value.instructionsTab.otherInstructions.map { it.id })
        assertEquals(3, viewModel.uiState.value.reportsTab.reportTypes.size)
        assertEquals(3, viewModel.uiState.value.contactsTab.threads.size)
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
    fun `home instruction shortcut opens instruction detail`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onHomeInstructionSelected()

        assertEquals(AppTab.INSTRUCTIONS, viewModel.uiState.value.selectedTab)
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
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

        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(listOf("instruction-patrol"), viewModel.uiState.value.instructionsTab.otherInstructions.map { it.id })

        viewModel.clear()
    }

    @Test
    fun `opening instruction thread switches to contacts tab with linked thread selected`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onInstructionSelected("instruction-gate-a")
        viewModel.onInstructionThreadOpened()

        assertEquals(AppTab.CONTACTS, viewModel.uiState.value.selectedTab)
        assertEquals("contact-gate-a", viewModel.uiState.value.contactsTab.selectedThread?.id)
        assertEquals(false, viewModel.uiState.value.instructionsTab.isShowingThread)
        viewModel.clear()
    }

    @Test
    fun `closing contact thread opened from instruction returns to instruction tab`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onInstructionSelected("instruction-gate-a")
        viewModel.onInstructionThreadOpened()
        viewModel.onContactBackToList()

        assertEquals(AppTab.INSTRUCTIONS, viewModel.uiState.value.selectedTab)
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals(null, viewModel.uiState.value.contactsTab.selectedThread)
        viewModel.clear()
    }

    @Test
    fun `report flow advances to submitted thread`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffSession = createSession(dispatcher),
            coroutineContext = dispatcher
        )

        viewModel.onReportTypeSelected("queue")
        viewModel.onReportCommentChanged("最後尾が歩道へ伸びています")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportPlaceSelected("place-gate-a")
        viewModel.onReportSubmitted()

        assertEquals(ReportFlowStep.THREAD, viewModel.uiState.value.reportsTab.step)
        assertEquals("Aゲート", viewModel.uiState.value.reportsTab.draft.selectedPlaceName)
        assertNotNull(viewModel.uiState.value.reportsTab.submittedThread)
        viewModel.clear()
    }

    @Test
    fun `contact flow can create new direct thread and send message`() = runTest {
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
        assertEquals("佐藤", viewModel.uiState.value.contactsTab.selectedThread?.title)
        assertEquals(4, viewModel.uiState.value.contactsTab.threads.size)
        assertEquals(
            "今どこにいますか",
            viewModel.uiState.value.contactsTab.selectedThread?.messages?.last()?.body
        )
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
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.featuredInstruction?.id)
        assertEquals(null, viewModel.uiState.value.instructionsTab.selectedInstruction)

        viewModel.clear()
    }

    private fun createSession(dispatcher: CoroutineContext): CurrentStaffSession = MockCurrentStaffSession(
        initialStaff = defaultMockStaffMembers(),
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
