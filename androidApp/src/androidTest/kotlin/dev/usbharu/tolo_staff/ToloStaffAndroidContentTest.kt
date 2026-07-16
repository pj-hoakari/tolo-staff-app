package dev.usbharu.tolo_staff

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetType
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.CurrentStaffUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.PlacementPhase
import dev.usbharu.tolo_staff.feature.appshell.RelatedReportUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.placementStatusFor
import org.junit.Rule
import org.junit.Test

class ToloStaffAndroidContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fourTabHierarchyIsPreserved() {
        setTestContent()

        composeRule.onNodeWithText("指示").performClick()
        composeRule.onNodeWithText("その他の指示").assertExists()
        composeRule.onNodeWithText("報告").performClick()
        composeRule.onNodeWithText("本部へ送る報告種別を選択します。").assertExists()
        composeRule.onNodeWithText("連絡").performClick()
        composeRule.onNodeWithText("現在のスレッドと新規連絡の開始").assertExists()
    }

    @Test
    fun homeShowsOnlyPlacementPointAndArrivalAction() {
        setTestContent()

        composeRule.onNodeWithText("南口").assertExists()
        composeRule.onNodeWithText("到着").assertExists()
        composeRule.onAllNodesWithContentDescription("app_shell_home_map").assertCountEquals(0)
    }

    @Test
    fun disabledOidcDoesNotExposeLogout() {
        setTestContent()

        composeRule.onAllNodesWithTag("auth_logout_button").assertCountEquals(0)
        composeRule.onAllNodesWithTag("auth_login_button").assertCountEquals(0)
    }

    @Test
    fun reportDetailHasNoContactThreadAction() {
        setTestContent()

        composeRule.onNodeWithText("報告").performClick()
        composeRule.onNodeWithContentDescription("related_report_report-1").performClick()
        composeRule.onNodeWithContentDescription("report_detail_screen").assertExists()
        composeRule.onAllNodesWithContentDescription("report_detail_open_thread_button").assertCountEquals(0)
    }

    @Test
    fun newContactOffersOnlyPlaceAndUserTargets() {
        setTestContent()

        composeRule.onNodeWithText("連絡").performClick()
        composeRule.onNodeWithContentDescription("contact_new_thread_button").performClick()
        composeRule.onNodeWithTag("contact_target_tabs").assertExists()
        composeRule.onNodeWithText("南口").assertExists()
        composeRule.onNodeWithText("担当場所").assertExists()
        composeRule.onNodeWithText("個人").assertExists()
        composeRule.onAllNodesWithContentDescription("contact_target_role").assertCountEquals(0)

        composeRule.onNodeWithText("個人").performClick()
        composeRule.onNodeWithText("佐藤").assertExists()
    }

    private fun setTestContent() {
        var state by mutableStateOf(testState())
        composeRule.setContent {
            ToloStaffAndroidContent(
                state = state,
                onTabSelected = { state = state.copy(selectedTab = it) },
                onReportSelected = { id ->
                    val report = state.reportsTab.relatedReports.first { it.reportId == id }
                    state = state.copy(
                        reportsTab = state.reportsTab.copy(
                            selectedReport = dev.usbharu.tolo_staff.feature.appshell.ReportDetailUiModel(
                                reportId = report.reportId,
                                title = report.title,
                                summary = report.summary,
                                severityLabel = report.severityLabel,
                                authorName = report.authorName,
                                targetLabel = report.targetLabel,
                                statusLabel = report.statusLabel,
                            )
                        )
                    )
                },
                onContactNewThreadStarted = {
                    state = state.copy(contactsTab = state.contactsTab.copy(isChoosingTargetType = true))
                },
                onContactTargetTypeSelected = { type ->
                    state = state.copy(contactsTab = state.contactsTab.copy(selectedTargetType = type))
                },
            )
        }
    }

    private fun testState(): AppShellUiState {
        val report = RelatedReportUiModel(
            reportId = "report-1",
            title = "混雑",
            summary = "南口の列が伸びています",
            severityLabel = "高",
            authorStaffId = "staff-1",
            authorName = "田中",
            targetLabel = "南口",
            statusLabel = "未対応",
            isAuthoredByCurrentStaff = true,
        )
        return AppShellUiState(
            currentStaff = CurrentStaffUiModel("staff-1", "田中"),
            homeOverview = AppShellHomeOverview(
                eventName = "夏祭り",
                placementName = "南口",
                placementStatus = placementStatusFor("assignment-1", 4, "南口", PlacementPhase.SCHEDULED),
            ),
            instructionsTab = InstructionsTabUiState(
                otherInstructions = listOf(
                    InstructionSummaryUiModel("instruction-1", "列整理", "南口", "対応中", "南口を整理してください")
                )
            ),
            reportsTab = ReportsTabUiState(
                availablePlaces = listOf(ContactTargetUiModel("point-1", ContactTargetType.PLACE, "南口")),
                relatedReports = listOf(report),
            ),
            contactsTab = ContactsTabUiState(
                threads = listOf(
                    ContactThreadSummaryUiModel(
                        "thread-1",
                        "南口連絡",
                        ContactTargetUiModel("point-1", ContactTargetType.PLACE, "南口"),
                        "了解しました",
                    )
                ),
                availableTargets = listOf(
                    ContactTargetUiModel("point-1", ContactTargetType.PLACE, "南口"),
                    ContactTargetUiModel("staff-2", ContactTargetType.USER, "佐藤"),
                ),
            ),
            isLoading = false,
        )
    }
}
