package dev.usbharu.tolo_staff

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellMapState
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadBackDestination
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetType
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.CurrentStaffUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.ReportDraftUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportFlowStep
import dev.usbharu.tolo_staff.feature.appshell.RelatedReportUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportTypeUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.ThreadMessageUiModel
import org.junit.Rule
import org.junit.Test

class ToloStaffAndroidContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomNavigationSwitchesTabs() {
        setTestContent()

        composeRule.onNodeWithText("指示").performClick()
        composeRule.onNodeWithText("その他の指示").assertExists()

        composeRule.onNodeWithText("報告").performClick()
        composeRule.onNodeWithText("本部へ送る報告種別を選択します。").assertExists()

        composeRule.onNodeWithText("連絡").performClick()
        composeRule.onNodeWithText("現在のスレッドと新規連絡の開始").assertExists()
    }

    @Test
    fun instructionsDetailReturnsToListOnSystemBack() {
        setTestContent()

        composeRule.onNodeWithText("指示").performClick()
        composeRule.onNodeWithContentDescription("instruction_row_instruction-2").performClick()
        composeRule.onNodeWithText("指示詳細").assertExists()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithText("その他の指示").assertExists()
    }

    @Test
    fun reportsSubmissionOpensContactThreadAndBackReturnsToReports() {
        setTestContent()

        composeRule.onNodeWithText("報告").performClick()
        composeRule.onNodeWithContentDescription("report_type_report-1").performClick()
        composeRule.onNodeWithText("対象場所を選ぶ").assertExists()
        composeRule.onNodeWithText("対象場所を選ぶ").performClick()
        composeRule.onNodeWithText("対象場所").assertExists()
        composeRule.onNodeWithText("南口").performClick()
        composeRule.onNodeWithText("本部へ送信").performClick()

        composeRule.onNodeWithContentDescription("contact_thread_detail").assertExists()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("本部へ送る報告種別を選択します。").assertExists()
    }

    @Test
    fun relatedReportOpensDetailThenContactThreadAndBackReturnsToDetail() {
        setTestContent()

        composeRule.onNodeWithText("報告").performClick()
        composeRule.onNodeWithContentDescription("related_report_report-1").performClick()
        composeRule.onNodeWithContentDescription("report_detail_screen").assertExists()
        composeRule.onNodeWithContentDescription("report_detail_open_thread_button").performClick()
        composeRule.onNodeWithContentDescription("contact_thread_detail").assertExists()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithContentDescription("report_detail_screen").assertExists()
    }

    @Test
    fun contactsDetailAndTargetSelectionReturnOnSystemBack() {
        setTestContent()

        composeRule.onNodeWithText("連絡").performClick()
        composeRule.onNodeWithContentDescription("contact_thread_thread-1").performClick()
        composeRule.onNodeWithContentDescription("contact_thread_detail").assertExists()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("現在のスレッドと新規連絡の開始").assertExists()

        composeRule.onNodeWithContentDescription("contact_new_thread_button").performClick()
        composeRule.onNodeWithContentDescription("contact_target_selection").assertExists()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("現在のスレッドと新規連絡の開始").assertExists()
    }

    @Test
    fun contactNewThreadFabIsHiddenOutsideListScreen() {
        setTestContent()

        composeRule.onNodeWithText("連絡").performClick()
        composeRule.onNodeWithContentDescription("contact_new_thread_button").performClick()
        composeRule.onAllNodesWithContentDescription("contact_new_thread_button").assertCountEquals(0)

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithContentDescription("contact_thread_thread-1").performClick()
        composeRule.onAllNodesWithContentDescription("contact_new_thread_button").assertCountEquals(0)
    }

    private fun setTestContent() {
        val featuredInstruction = InstructionSummaryUiModel(
            id = "instruction-1",
            title = "南口の導線を整理",
            targetName = "南口",
            priorityLabel = "高",
            statusLabel = "対応中",
            preview = "開場前に列整理を開始してください",
        )
        val otherInstruction = InstructionSummaryUiModel(
            id = "instruction-2",
            title = "備品確認",
            targetName = "本部",
            priorityLabel = "",
            statusLabel = "対応中",
            preview = "予備バッテリーの残数を共有",
        )
        val instructionDetail = InstructionDetailUiModel(
            id = otherInstruction.id,
            title = otherInstruction.title,
            body = "予備バッテリーの残数を確認して本部へ共有してください",
            target = ContactTargetUiModel(
                id = "hq",
                type = ContactTargetType.HEADQUARTERS,
                displayName = "本部",
            ),
            priorityLabel = "",
            statusLabel = "対応中",
        )
        val reportTypes = listOf(
            ReportTypeUiModel("report-1", "巡回報告", "一定時間ごとの現場報告")
        )
        val reportPlaces = listOf(
            ContactTargetUiModel("place-1", ContactTargetType.PLACE, "南口", "入場導線")
        )
        val relatedReport = RelatedReportUiModel(
            reportId = "report-1",
            threadId = "report-thread-1",
            title = "巡回報告",
            summary = "南口の入場列は安定しています",
            priorityLabel = "通常",
            authorStaffId = "tanaka",
            authorName = "田中",
            targetLabel = "南口",
            timeLabel = "2026-06-19T09:00:00Z",
            isAuthoredByCurrentStaff = true,
        )
        val contactThread = ContactThreadDetailUiModel(
            id = "thread-1",
            title = "本部連絡",
            target = ContactTargetUiModel("target-1", ContactTargetType.HEADQUARTERS, "本部"),
            messages = listOf(
                ThreadMessageUiModel(
                    id = "message-1",
                    senderName = "本部",
                    body = "状況を共有してください",
                    timeLabel = "10:30",
                )
            ),
        )
        val summaryThread = ContactThreadSummaryUiModel(
            id = contactThread.id,
            title = contactThread.title,
            target = contactThread.target,
            lastMessagePreview = "状況を共有してください",
            unreadCount = 1,
        )

        composeRule.setContent {
            var state by mutableStateOf(
                AppShellUiState(
                    homeOverview = AppShellHomeOverview(
                        eventName = "夏祭り",
                        eventTime = "10:00 - 18:00",
                        placementName = "南口",
                        placementDetail = "導線整理担当",
                        currentInstruction = featuredInstruction.preview,
                        currentInstructionId = featuredInstruction.id,
                        currentInstructionTitle = featuredInstruction.title,
                        currentInstructionTargetName = featuredInstruction.targetName,
                        currentInstructionPriorityLabel = featuredInstruction.priorityLabel,
                        currentInstructionStatusLabel = featuredInstruction.statusLabel,
                        pendingReportLabel = "次の定時報告まで 12 分",
                        mapState = AppShellMapState(
                            venueName = "会場 南口",
                            latitude = 35.0,
                            longitude = 139.0,
                        ),
                    ),
                    currentPlacementName = "南口",
                    currentStaff = CurrentStaffUiModel(
                        staffId = "tanaka",
                        displayName = "田中",
                        roleLabel = "導線担当",
                    ),
                    selectedTab = AppTab.HOME,
                    instructionsTab = InstructionsTabUiState(
                        instructions = listOf(featuredInstruction, otherInstruction),
                        featuredInstruction = featuredInstruction,
                        otherInstructions = listOf(otherInstruction),
                    ),
                    reportsTab = ReportsTabUiState(
                        reportTypes = reportTypes,
                        availablePlaces = reportPlaces,
                        draft = ReportDraftUiModel(urgencyLabel = "通常"),
                        step = ReportFlowStep.TYPE_SELECTION,
                        relatedReports = listOf(relatedReport),
                    ),
                    contactsTab = ContactsTabUiState(
                        threads = listOf(summaryThread),
                        availableTargets = reportPlaces + listOf(
                            ContactTargetUiModel("user-1", ContactTargetType.USER, "佐藤", "本部")
                        ),
                    ),
                    isLoading = false,
                )
            )

            ToloStaffAndroidContent(
                state = state,
                onTabSelected = { tab -> state = state.copy(selectedTab = tab) },
                onHomeInstructionSelected = {
                    state = state.copy(
                        selectedTab = AppTab.INSTRUCTIONS,
                        instructionsTab = state.instructionsTab.copy(selectedInstruction = instructionDetail),
                    )
                },
                onInstructionSelected = {
                    state = state.copy(
                        selectedTab = AppTab.INSTRUCTIONS,
                        instructionsTab = state.instructionsTab.copy(selectedInstruction = instructionDetail),
                    )
                },
                onInstructionThreadOpened = {
                    state = state.copy(
                        selectedTab = AppTab.CONTACTS,
                        contactsTab = state.contactsTab.copy(
                            selectedThread = contactThread,
                            selectedThreadBackDestination = ContactThreadBackDestination.INSTRUCTIONS,
                        ),
                    )
                },
                onInstructionDetailClosed = {
                    state = state.copy(
                        instructionsTab = state.instructionsTab.copy(selectedInstruction = null),
                    )
                },
                onInstructionStatusUpdated = {},
                onReportTypeSelected = {
                    state = state.copy(
                        selectedTab = AppTab.REPORTS,
                        reportsTab = state.reportsTab.copy(
                            step = ReportFlowStep.DRAFT_INPUT,
                            draft = state.reportsTab.draft.copy(selectedTypeId = it, templateText = reportTypes.first().detailText),
                        ),
                    )
                },
                onReportCommentChanged = {
                    state = state.copy(reportsTab = state.reportsTab.copy(draft = state.reportsTab.draft.copy(comment = it)))
                },
                onReportSelected = {
                    state = state.copy(
                        selectedTab = AppTab.REPORTS,
                        reportsTab = state.reportsTab.copy(
                            selectedReport = ReportDetailUiModel(
                                reportId = relatedReport.reportId,
                                threadId = relatedReport.threadId,
                                title = relatedReport.title,
                                summary = relatedReport.summary,
                                priorityLabel = relatedReport.priorityLabel,
                                authorName = relatedReport.authorName,
                                targetLabel = relatedReport.targetLabel,
                                timeLabel = relatedReport.timeLabel,
                                isAuthoredByCurrentStaff = true,
                                detailPlaceholderMessage = "詳細情報は今後の API 連携で表示予定です。現在は概要のみ確認できます。",
                            ),
                        )
                    )
                },
                onReportDetailClosed = {
                    state = state.copy(
                        reportsTab = state.reportsTab.copy(selectedReport = null),
                    )
                },
                onReportThreadOpened = {
                    state = state.copy(
                        selectedTab = AppTab.CONTACTS,
                        contactsTab = state.contactsTab.copy(
                            selectedThread = contactThread.copy(
                                id = relatedReport.threadId,
                                title = relatedReport.title,
                                target = ContactTargetUiModel(
                                    relatedReport.threadId,
                                    ContactTargetType.HEADQUARTERS,
                                    relatedReport.targetLabel,
                                ),
                                messages = listOf(
                                    ThreadMessageUiModel(
                                        id = "report-message-1",
                                        senderName = relatedReport.authorName,
                                        body = relatedReport.summary,
                                        timeLabel = relatedReport.timeLabel,
                                        isCurrentUser = true,
                                    )
                                ),
                            ),
                            selectedThreadBackDestination = ContactThreadBackDestination.REPORT_DETAIL,
                        )
                    )
                },
                onReportUrgencySelected = {
                    state = state.copy(reportsTab = state.reportsTab.copy(draft = state.reportsTab.draft.copy(urgencyLabel = it)))
                },
                onReportImageToggleChanged = {
                    state = state.copy(reportsTab = state.reportsTab.copy(draft = state.reportsTab.draft.copy(includesImage = it)))
                },
                onReportLocationToggleChanged = {
                    state = state.copy(reportsTab = state.reportsTab.copy(draft = state.reportsTab.draft.copy(includesLocation = it)))
                },
                onReportContinueToPlaceSelection = {
                    state = state.copy(
                        selectedTab = AppTab.REPORTS,
                        reportsTab = state.reportsTab.copy(step = ReportFlowStep.PLACE_SELECTION),
                    )
                },
                onReportPlaceSelected = {
                    state = state.copy(
                        reportsTab = state.reportsTab.copy(
                            draft = state.reportsTab.draft.copy(selectedPlaceId = it, selectedPlaceName = "南口")
                        ),
                    )
                },
                onReportSubmitted = {
                    state = state.copy(
                        selectedTab = AppTab.CONTACTS,
                        reportsTab = state.reportsTab.copy(
                            draft = ReportDraftUiModel(urgencyLabel = "通常"),
                            step = ReportFlowStep.TYPE_SELECTION,
                        ),
                        contactsTab = state.contactsTab.copy(
                            threads = state.contactsTab.threads + ContactThreadSummaryUiModel(
                                id = "report-report-1-place-1",
                                title = "巡回報告 / 南口",
                                target = ContactTargetUiModel("place-1", ContactTargetType.PLACE, "南口", "本部"),
                                lastMessagePreview = "巡回報告 [通常]",
                            ),
                            selectedThread = ContactThreadDetailUiModel(
                                id = "report-report-1-place-1",
                                title = "巡回報告 / 南口",
                                target = ContactTargetUiModel("place-1", ContactTargetType.PLACE, "南口", "本部"),
                                messages = listOf(
                                    ThreadMessageUiModel(
                                        id = "report-message-submitted",
                                        senderName = "田中",
                                        body = "巡回報告 [通常]",
                                        isCurrentUser = true,
                                    )
                                ),
                            ),
                            selectedThreadBackDestination = ContactThreadBackDestination.REPORTS,
                        )
                    )
                },
                onReportBack = {
                    state = state.copy(
                        reportsTab = state.reportsTab.copy(
                            step = when (state.reportsTab.step) {
                                ReportFlowStep.TYPE_SELECTION -> ReportFlowStep.TYPE_SELECTION
                                ReportFlowStep.DRAFT_INPUT -> ReportFlowStep.TYPE_SELECTION
                                ReportFlowStep.PLACE_SELECTION -> ReportFlowStep.DRAFT_INPUT
                            }
                        )
                    )
                },
                onContactThreadSelected = {
                    state = state.copy(
                        selectedTab = AppTab.CONTACTS,
                        contactsTab = state.contactsTab.copy(
                            selectedThread = contactThread,
                            selectedThreadBackDestination = ContactThreadBackDestination.NONE,
                        ),
                    )
                },
                onContactBackToList = {
                    state = state.copy(
                        selectedTab = when (state.contactsTab.selectedThreadBackDestination) {
                            ContactThreadBackDestination.NONE -> state.selectedTab
                            ContactThreadBackDestination.INSTRUCTIONS -> AppTab.INSTRUCTIONS
                            ContactThreadBackDestination.REPORTS -> AppTab.REPORTS
                            ContactThreadBackDestination.REPORT_DETAIL -> AppTab.REPORTS
                        },
                        contactsTab = state.contactsTab.copy(
                            selectedThread = null,
                            isChoosingTargetType = false,
                            selectedTargetType = null,
                            selectedThreadBackDestination = ContactThreadBackDestination.NONE,
                        )
                    )
                },
                onContactNewThreadStarted = {
                    state = state.copy(
                        selectedTab = AppTab.CONTACTS,
                        contactsTab = state.contactsTab.copy(
                            isChoosingTargetType = true,
                            selectedThreadBackDestination = ContactThreadBackDestination.NONE,
                        ),
                    )
                },
                onContactTargetTypeSelected = {
                    state = state.copy(contactsTab = state.contactsTab.copy(selectedTargetType = it))
                },
                onContactTargetSelected = {},
                onContactDraftChanged = {
                    state = state.copy(
                        contactsTab = state.contactsTab.copy(
                            selectedThread = state.contactsTab.selectedThread?.copy(draftMessage = it)
                        )
                    )
                },
                onContactSendClicked = {},
                onCurrentStaffSelected = {},
            )
        }
    }
}
