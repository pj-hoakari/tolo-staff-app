package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.AppShellOperationsProjection
import dev.usbharu.tolo_staff.streaming.CurrentStaffProvider
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @Test
    fun `initial state reflects streamed overview`() = runTest {
        val repository = FakeOperationsOverviewRepository(
            AppShellOperationsProjection(
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
        val viewModel = AppShellViewModel(
            overviewRepository = repository,
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
        )

        assertEquals("Gate A", viewModel.uiState.value.currentPlacementName)
        assertEquals("Tolo Staff Demo 2026", viewModel.uiState.value.homeOverview.eventName)
        assertEquals("North entrance", viewModel.uiState.value.homeOverview.placementDetail)
        assertEquals("Shift update: Move barricades", viewModel.uiState.value.homeOverview.currentInstruction)
        assertNotNull(viewModel.uiState.value.instructionsTab.selectedInstruction)
        assertEquals(3, viewModel.uiState.value.reportsTab.reportTypes.size)
        assertEquals(3, viewModel.uiState.value.contactsTab.threads.size)
        assertEquals(AppTab.HOME, viewModel.uiState.value.selectedTab)
        assertEquals(false, viewModel.uiState.value.isLoading)
        viewModel.clear()
    }

    @Test
    fun `tab selection updates selected tab`() = runTest {
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
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
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
        )

        viewModel.onHomeInstructionSelected()

        assertEquals(AppTab.INSTRUCTIONS, viewModel.uiState.value.selectedTab)
        assertEquals("instruction-gate-a", viewModel.uiState.value.instructionsTab.selectedInstruction?.id)
        assertEquals(false, viewModel.uiState.value.instructionsTab.isShowingThread)
        viewModel.clear()
    }

    @Test
    fun `report flow advances to submitted thread`() = runTest {
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
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
        val viewModel = AppShellViewModel(
            overviewRepository = FakeOperationsOverviewRepository(),
            currentStaffProvider = FixedCurrentStaffProviderForTest(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler)
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
}

private class FakeOperationsOverviewRepository(
    initialProjection: AppShellOperationsProjection = AppShellOperationsProjection(
        homeOverview = AppShellHomeOverview(),
        currentPlacementName = "未配属"
    )
) : OperationsOverviewRepository {
    private val projectionFlow = MutableStateFlow(initialProjection)

    override fun observeOverview(currentStaffId: String): Flow<AppShellOperationsProjection> = projectionFlow
}

private class FixedCurrentStaffProviderForTest : CurrentStaffProvider {
    override val currentStaffId: String = "tanaka"
}
