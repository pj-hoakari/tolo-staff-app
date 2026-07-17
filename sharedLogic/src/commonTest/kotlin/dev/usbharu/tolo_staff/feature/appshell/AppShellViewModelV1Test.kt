package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.error.AppErrorPresentation
import dev.usbharu.tolo_staff.error.AppErrorReporter
import dev.usbharu.tolo_staff.error.AppErrorSource
import dev.usbharu.tolo_staff.streaming.CurrentStaffMember
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.streaming.TestOperationsStreamDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelV1Test {
    @Test
    fun `error reported before view model creation is forwarded and acknowledged by UI`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportConnectionFailure(
            AppErrorSource.FIREBASE,
            operation = "Firebase session",
            connectionKey = "firebase.session",
        )
        val viewModel = AppShellViewModel(
            errorReporter = reporter,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        val effect = viewModel.effectFlow.first()

        assertEquals(AppErrorSource.FIREBASE, effect.source)
        assertEquals(AppErrorPresentation.TRANSIENT, effect.presentation)
        viewModel.acknowledgeError(effect.eventId)
        viewModel.clear()
    }

    @Test
    fun `pending errors are forwarded one at a time after acknowledgement`() = runTest {
        val reporter = AppErrorReporter()
        reporter.reportConnectionFailure(
            AppErrorSource.GRPC,
            operation = "gRPC startup",
            connectionKey = "test.grpc",
        )
        reporter.reportConnectionFailure(
            AppErrorSource.FIREBASE,
            operation = "Firebase startup",
            connectionKey = "test.firebase",
        )
        val viewModel = AppShellViewModel(
            errorReporter = reporter,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        val received = mutableListOf<AppShellEffect>()
        val collectionJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effectFlow.collect(received::add)
        }

        advanceUntilIdle()
        assertEquals(listOf(AppErrorSource.GRPC), received.map(AppShellEffect::source))

        viewModel.acknowledgeError(received.single().eventId)
        advanceUntilIdle()
        assertEquals(
            listOf(AppErrorSource.GRPC, AppErrorSource.FIREBASE),
            received.map(AppShellEffect::source),
        )

        collectionJob.cancel()
        viewModel.clear()
    }

    @Test
    fun `new contact opens with place target tab selected`() = runTest {
        val viewModel = AppShellViewModel(
            coroutineContext = StandardTestDispatcher(testScheduler),
        )

        viewModel.onContactNewThreadStarted()

        assertEquals(ContactTargetType.PLACE, viewModel.uiState.value.contactsTab.selectedTargetType)
        assertEquals(true, viewModel.uiState.value.contactsTab.isChoosingTargetType)
        viewModel.clear()
    }

    @Test
    fun `submitted report is inserted immediately and stays on report list`() = runTest {
        val dataSource = TestOperationsStreamDataSource(
            points = listOf(OperationPoint(pointId = "point-1", name = "南口")),
            staff = listOf(OperationStaff(staffId = "staff-1", name = "田中")),
        )
        val reports = FakeReportRepository()
        val viewModel = AppShellViewModel(
            overviewRepository = OperationsOverviewRepositoryImpl(dataSource),
            dataSource = dataSource,
            currentStaffSession = MockCurrentStaffSession(listOf(CurrentStaffMember("staff-1", "田中"))),
            reportRepository = reports,
            coroutineContext = StandardTestDispatcher(testScheduler),
        )
        advanceUntilIdle()

        viewModel.onTabSelected(AppTab.REPORTS)
        viewModel.onReportTypeSelected("congestion")
        viewModel.onReportCommentChanged("列が伸びています")
        viewModel.onReportContinueToPlaceSelection()
        viewModel.onReportPlaceSelected("point-1")
        viewModel.onReportSubmitted()
        advanceUntilIdle()

        assertEquals(AppTab.REPORTS, viewModel.uiState.value.selectedTab)
        assertEquals(ReportFlowStep.TYPE_SELECTION, viewModel.uiState.value.reportsTab.step)
        assertNull(viewModel.uiState.value.reportsTab.selectedReport)
        assertEquals("report-1", viewModel.uiState.value.reportsTab.relatedReports.first().reportId)
        assertEquals("point-1", reports.submittedPointId)
        assertEquals(ReportCategoryValue.CONGESTION, reports.submittedCategory)
        viewModel.clear()
    }

    private class FakeReportRepository : ReportRepository {
        private val flow = MutableStateFlow<List<RelevantReport>>(emptyList())
        var submittedPointId: String? = null
        var submittedCategory: ReportCategoryValue? = null

        override fun observeRelevantReports(currentStaffId: String, assignedPointIds: Set<String>): Flow<List<RelevantReport>> = flow
        override suspend fun listRelevantReports(currentStaffId: String, assignedPointIds: Set<String>) = flow.value

        override suspend fun submit(
            category: ReportCategoryValue,
            severity: ReportSeverityValue,
            pointId: String,
            description: String,
        ): RelevantReport {
            submittedCategory = category
            submittedPointId = pointId
            return RelevantReport(
                reportId = "report-1",
                authorStaffId = "staff-1",
                pointId = pointId,
                category = category,
                severity = severity,
                statusLabel = "未対応",
                summary = description,
            )
        }
    }
}
