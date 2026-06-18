package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.streaming.CurrentStaffMember
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

class AppShellViewModel(
    private val overviewRepository: OperationsOverviewRepository = OperationsOverviewRepositoryImpl(
        dataSource = NoOpOperationsStreamDataSource()
    ),
    private val currentStaffSession: CurrentStaffSession = MockCurrentStaffSession(),
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<AppShellUiState, Unit>(
    initialState = initialState(
        currentStaff = currentStaffSession.currentStaffSnapshot,
        availableStaff = currentStaffSession.availableStaff.value,
    ),
    coroutineContext = coroutineContext
) {
    private var overviewJob: Job? = null
    private var staffSessionJob: Job? = null
    private var nextContactMessageId = 1L
    private var observedStaffId: String? = null

    init {
        staffSessionJob = combine(
            currentStaffSession.currentStaff,
            currentStaffSession.availableStaff,
        ) { currentStaff, availableStaff ->
            currentStaff to availableStaff
        }
            .onEach { (currentStaff, availableStaff) ->
                val didChange = observedStaffId != currentStaff.staffId
                observedStaffId = currentStaff.staffId

                updateState { state ->
                    if (didChange) {
                        initialState(
                            currentStaff = currentStaff,
                            availableStaff = availableStaff,
                        ).copy(selectedTab = state.selectedTab)
                    } else {
                        state.copy(
                            currentStaff = currentStaff.toUiModel(),
                            availableStaff = availableStaff.map { it.toUiModel() },
                        )
                    }
                }

                if (didChange) {
                    observeOverview(currentStaff.staffId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCurrentStaffSelected(staffId: String) {
        currentStaffSession.selectStaff(staffId)
    }

    fun onTabSelected(tab: AppTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    fun onHomeInstructionSelected() {
        val selectedInstruction = currentState.instructionsTab.selectedInstruction
            ?: instructionDetailFor(currentState.instructionsTab.instructions.firstOrNull()?.id)
            ?: return
        updateState {
            it.copy(
                selectedTab = AppTab.INSTRUCTIONS,
                instructionsTab = it.instructionsTab.copy(
                    selectedInstruction = selectedInstruction,
                    isShowingThread = false,
                )
            )
        }
    }

    fun onInstructionSelected(instructionId: String) {
        val detail = instructionDetailFor(instructionId) ?: return
        updateState {
            it.copy(
                instructionsTab = it.instructionsTab.copy(
                    selectedInstruction = detail,
                    isShowingThread = false,
                )
            )
        }
    }

    fun onInstructionThreadOpened() {
        val selectedInstruction = currentState.instructionsTab.selectedInstruction ?: return
        val threadSummary = contactSummaryForInstruction(selectedInstruction)
        val threadDetail = contactDetailFor(threadSummary)
        updateState { state ->
            state.copy(
                selectedTab = AppTab.CONTACTS,
                instructionsTab = state.instructionsTab.copy(isShowingThread = false),
                homeOverview = state.homeOverview.copy(unreadContactCount = 0),
                contactsTab = state.contactsTab.copy(
                    selectedThread = threadDetail,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    shouldReturnToInstructionOnBack = true,
                    threads = mergeContactSummary(
                        current = state.contactsTab.threads,
                        threadId = threadSummary.id,
                        title = threadSummary.title,
                        target = threadSummary.target,
                        preview = threadSummary.lastMessagePreview,
                        isFormerAssignment = threadSummary.isFormerAssignment,
                    ).map { summary ->
                        if (summary.id == threadSummary.id) summary.copy(unreadCount = 0) else summary
                    }
                )
            )
        }
    }

    fun onInstructionDetailClosed() {
        updateState {
            it.copy(
                instructionsTab = it.instructionsTab.copy(
                    selectedInstruction = null,
                    isShowingThread = false,
                )
            )
        }
    }

    fun onInstructionStatusUpdated(status: InstructionProgressStatus) {
        val selectedInstruction = currentState.instructionsTab.selectedInstruction ?: return
        val statusLabel = status.toStatusLabel()
        updateState { state ->
            state.copy(
                homeOverview = state.homeOverview.copy(
                    currentInstruction = selectedInstruction.title,
                    currentInstructionId = selectedInstruction.id,
                ),
                instructionsTab = state.instructionsTab.copy(
                    instructions = state.instructionsTab.instructions.map { summary ->
                        if (summary.id == selectedInstruction.id) {
                            summary.copy(statusLabel = statusLabel)
                        } else {
                            summary
                        }
                    },
                    selectedInstruction = selectedInstruction.copy(
                        statusLabel = statusLabel,
                        participants = selectedInstruction.participants.map { participant ->
                            if (participant.isCurrentStaff) {
                                participant.copy(statusLabel = statusLabel)
                            } else {
                                participant
                            }
                        }
                    )
                )
            )
        }
    }

    fun onReportTypeSelected(typeId: String) {
        val reportType = currentState.reportsTab.reportTypes.firstOrNull { it.id == typeId } ?: return
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    step = ReportFlowStep.DRAFT_INPUT,
                    draft = it.reportsTab.draft.copy(
                        selectedTypeId = reportType.id,
                        templateText = reportType.detailText,
                    )
                )
            )
        }
    }

    fun onReportCommentChanged(comment: String) {
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    draft = it.reportsTab.draft.copy(comment = comment)
                )
            )
        }
    }

    fun onReportUrgencySelected(label: String) {
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    draft = it.reportsTab.draft.copy(urgencyLabel = label)
                )
            )
        }
    }

    fun onReportImageToggleChanged(isEnabled: Boolean) {
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    draft = it.reportsTab.draft.copy(includesImage = isEnabled)
                )
            )
        }
    }

    fun onReportLocationToggleChanged(isEnabled: Boolean) {
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    draft = it.reportsTab.draft.copy(includesLocation = isEnabled)
                )
            )
        }
    }

    fun onReportContinueToPlaceSelection() {
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(step = ReportFlowStep.PLACE_SELECTION)
            )
        }
    }

    fun onReportPlaceSelected(placeId: String) {
        val place = currentState.reportsTab.availablePlaces.firstOrNull { it.id == placeId } ?: return
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    draft = it.reportsTab.draft.copy(
                        selectedPlaceId = place.id,
                        selectedPlaceName = place.displayName,
                    )
                )
            )
        }
    }

    fun onReportSubmitted() {
        val draft = currentState.reportsTab.draft
        val selectedType = currentState.reportsTab.reportTypes.firstOrNull { it.id == draft.selectedTypeId }
        val placeName = draft.selectedPlaceName ?: return
        val currentStaff = currentState.currentStaff
        updateState { state ->
            state.copy(
                homeOverview = state.homeOverview.copy(
                    pendingReportLabel = "$placeName に関する${selectedType?.title ?: "報告"}を本部へ送信済み"
                ),
                reportsTab = state.reportsTab.copy(
                    step = ReportFlowStep.THREAD,
                    submittedThread = ReportThreadUiModel(
                        id = "report-thread-$placeName",
                        title = "${selectedType?.title ?: "報告"}スレッド",
                        targetLabel = "宛先: 本部 / 対象場所: $placeName",
                        lastSubmittedSummary = draft.comment.ifBlank { draft.templateText },
                        messages = listOf(
                            ThreadMessageUiModel(
                                id = "report-1",
                                senderName = currentStaff.displayName,
                                senderRoleLabel = currentStaff.roleLabel ?: state.currentPlacementName,
                                body = draft.comment.ifBlank { draft.templateText },
                                timeLabel = "たった今",
                                isCurrentUser = true,
                            ),
                            ThreadMessageUiModel(
                                id = "report-2",
                                senderName = "運営本部",
                                senderRoleLabel = "HEADQUARTERS",
                                body = "状況を確認しています。追加の情報が必要な場合はこのスレッドで連絡します。",
                                timeLabel = "たった今",
                            )
                        )
                    )
                )
            )
        }
    }

    fun onReportBack() {
        updateState { state ->
            val reportsTab = state.reportsTab
            val nextStep = when (reportsTab.step) {
                ReportFlowStep.THREAD -> ReportFlowStep.PLACE_SELECTION
                ReportFlowStep.PLACE_SELECTION -> ReportFlowStep.DRAFT_INPUT
                ReportFlowStep.DRAFT_INPUT -> ReportFlowStep.TYPE_SELECTION
                ReportFlowStep.TYPE_SELECTION -> ReportFlowStep.TYPE_SELECTION
            }
            state.copy(
                reportsTab = reportsTab.copy(
                    step = nextStep,
                    submittedThread = if (nextStep == ReportFlowStep.THREAD) reportsTab.submittedThread else null
                )
            )
        }
    }

    fun onContactThreadSelected(threadId: String) {
        val summary = currentState.contactsTab.threads.firstOrNull { it.id == threadId } ?: return
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    selectedThread = contactDetailFor(summary),
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    shouldReturnToInstructionOnBack = false,
                    threads = it.contactsTab.threads.map { thread ->
                        if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
                    }
                )
            )
        }
    }

    fun onContactBackToList() {
        updateState { state ->
            val shouldReturnToInstruction = state.contactsTab.shouldReturnToInstructionOnBack
            state.copy(
                selectedTab = if (shouldReturnToInstruction) AppTab.INSTRUCTIONS else state.selectedTab,
                contactsTab = state.contactsTab.copy(
                    selectedThread = null,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    shouldReturnToInstructionOnBack = false,
                )
            )
        }
    }

    fun onContactNewThreadStarted() {
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    selectedThread = null,
                    isChoosingTargetType = true,
                    selectedTargetType = null,
                    shouldReturnToInstructionOnBack = false,
                )
            )
        }
    }

    fun onContactTargetTypeSelected(type: ContactTargetType) {
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(selectedTargetType = type)
            )
        }
    }

    fun onContactTargetSelected(targetId: String) {
        val contactsTab = currentState.contactsTab
        val selectedTargetType = contactsTab.selectedTargetType ?: return
        val target = contactsTab.availableTargets.firstOrNull {
            it.id == targetId && it.type == selectedTargetType
        } ?: return
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    isChoosingTargetType = false,
                    shouldReturnToInstructionOnBack = false,
                    selectedThread = ContactThreadDetailUiModel(
                        id = "draft-${target.id}",
                        title = target.displayName,
                        target = target,
                        messages = listOf(
                            ThreadMessageUiModel(
                                id = "system-${target.id}",
                                senderName = "System",
                                body = "${target.displayName} への新規連絡を開始しました。",
                                timeLabel = "たった今",
                                isSystemEvent = true,
                            )
                        )
                    )
                )
            )
        }
    }

    fun onContactDraftChanged(text: String) {
        val selectedThread = currentState.contactsTab.selectedThread ?: return
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    selectedThread = selectedThread.copy(draftMessage = text)
                )
            )
        }
    }

    fun onContactSendClicked() {
        val selectedThread = currentState.contactsTab.selectedThread ?: return
        val draftMessage = selectedThread.draftMessage.trim()
        if (draftMessage.isEmpty()) {
            return
        }
        val currentStaff = currentState.currentStaff
        val message = ThreadMessageUiModel(
            id = "contact-local-${nextContactMessageId++}",
            senderName = currentStaff.displayName,
            senderRoleLabel = currentStaff.roleLabel ?: currentState.currentPlacementName,
            body = draftMessage,
            timeLabel = "たった今",
            isCurrentUser = true,
        )
        updateState { state ->
            val updatedDetail = selectedThread.copy(
                draftMessage = "",
                messages = selectedThread.messages + message,
            )
            state.copy(
                homeOverview = state.homeOverview.copy(
                    unreadContactCount = 0
                ),
                contactsTab = state.contactsTab.copy(
                    selectedThread = updatedDetail,
                    threads = mergeContactSummary(
                        state.contactsTab.threads,
                        updatedDetail.id,
                        updatedDetail.title,
                        updatedDetail.target,
                        draftMessage,
                        updatedDetail.isFormerAssignment,
                    )
                )
            )
        }
    }

    override fun clear() {
        overviewJob?.cancel()
        staffSessionJob?.cancel()
        super.clear()
    }

    private fun observeOverview(currentStaffId: String) {
        overviewJob?.cancel()
        overviewJob = overviewRepository.observeOverview(currentStaffId)
            .onEach { projection ->
                updateState {
                    it.copy(
                        homeOverview = projection.homeOverview,
                        currentPlacementName = projection.currentPlacementName,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            .catch { throwable ->
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "ホーム情報の購読に失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun instructionDetailFor(instructionId: String?): InstructionDetailUiModel? =
        when (instructionId) {
            "instruction-gate-a" -> primaryInstructionDetail(currentState.currentStaff)
            "instruction-patrol" -> secondaryInstructionDetail()
            else -> null
        }

    private fun contactDetailFor(summary: ContactThreadSummaryUiModel): ContactThreadDetailUiModel =
        ContactThreadDetailUiModel(
            id = summary.id,
            title = summary.title,
            target = summary.target,
            isFormerAssignment = summary.isFormerAssignment,
            canReply = true,
            messages = when (summary.id) {
                "contact-gate-a" -> listOf(
                    ThreadMessageUiModel(
                        id = "gate-a-1",
                        senderName = "運営本部",
                        senderRoleLabel = "HEADQUARTERS",
                        body = "Aゲート前の導線を右側へ広げてください。",
                        timeLabel = "10:15",
                    ),
                    ThreadMessageUiModel(
                        id = "gate-a-2",
                        senderName = currentState.currentStaff.displayName,
                        senderRoleLabel = currentState.currentStaff.roleLabel ?: "Aゲート担当",
                        body = "了解しました。カラーコーンを追加します。",
                        timeLabel = "10:17",
                        isCurrentUser = true,
                    )
                )
                "contact-hq" -> listOf(
                    ThreadMessageUiModel(
                        id = "hq-1",
                        senderName = "運営本部",
                        senderRoleLabel = "HEADQUARTERS",
                        body = "混雑報告があればこのスレッドへ送ってください。",
                        timeLabel = "09:40",
                    )
                )
                else -> listOf(
                    ThreadMessageUiModel(
                        id = "former-1",
                        senderName = "山田",
                        senderRoleLabel = "前Aゲート担当",
                        body = "14:20時点では列が右側に伸びていました。",
                        timeLabel = "14:21",
                    )
                )
            }
        )

    private fun contactSummaryForInstruction(
        instruction: InstructionDetailUiModel
    ): ContactThreadSummaryUiModel {
        val existingThread = currentState.contactsTab.threads.firstOrNull { summary ->
            summary.target.id == instruction.target.id
        }
        if (existingThread != null) {
            return existingThread
        }
        return ContactThreadSummaryUiModel(
            id = "instruction-thread-${instruction.target.id}",
            title = instruction.target.displayName,
            target = instruction.target,
            lastMessagePreview = instruction.body,
        )
    }

    private companion object {
        fun initialState(
            currentStaff: CurrentStaffMember,
            availableStaff: List<CurrentStaffMember>,
        ): AppShellUiState =
            AppShellUiState(
                currentStaff = currentStaff.toUiModel(),
                availableStaff = availableStaff.map { it.toUiModel() },
                instructionsTab = initialInstructionsState(currentStaff.toUiModel()),
                reportsTab = initialReportsState(),
                contactsTab = initialContactsState(),
                isLoading = true,
            )

        fun initialInstructionsState(currentStaff: CurrentStaffUiModel): InstructionsTabUiState {
            val detail = primaryInstructionDetail(currentStaff)
            return InstructionsTabUiState(
                instructions = listOf(
                    InstructionSummaryUiModel(
                        id = detail.id,
                        title = detail.title,
                        targetName = detail.target.displayName,
                        priorityLabel = detail.priorityLabel,
                        statusLabel = detail.statusLabel,
                        preview = detail.body,
                        unreadCount = 1,
                    ),
                    InstructionSummaryUiModel(
                        id = "instruction-patrol",
                        title = "巡回スタッフは西ホール通路の滞留を確認",
                        targetName = "巡回担当",
                        priorityLabel = "通常",
                        statusLabel = "未確認",
                        preview = "通路の滞留が増えた場合は本部へ即時報告してください。",
                    )
                ),
                selectedInstruction = detail,
            )
        }

        fun initialReportsState(): ReportsTabUiState =
            ReportsTabUiState(
                reportTypes = listOf(
                    ReportTypeUiModel("queue", "混雑報告", "列の長さ、導線、増員要否を記録します。"),
                    ReportTypeUiModel("incident", "トラブル報告", "転倒、迷子、クレームなどの状況を本部へ共有します。"),
                    ReportTypeUiModel("inventory", "備品依頼", "カラーコーン、案内板、テープなどの補充を依頼します。"),
                ),
                availablePlaces = listOf(
                    ContactTargetUiModel("place-gate-a", ContactTargetType.PLACE, "Aゲート"),
                    ContactTargetUiModel("place-gate-b", ContactTargetType.PLACE, "Bゲート"),
                    ContactTargetUiModel("place-tail", ContactTargetType.PLACE, "最後尾"),
                )
            )

        fun initialContactsState(): ContactsTabUiState =
            ContactsTabUiState(
                threads = listOf(
                    ContactThreadSummaryUiModel(
                        id = "contact-gate-a",
                        title = "Aゲート担当",
                        target = ContactTargetUiModel("place-gate-a", ContactTargetType.PLACE, "Aゲート担当", "現担当 3名"),
                        lastMessagePreview = "了解しました。カラーコーンを追加します。",
                        unreadCount = 2,
                    ),
                    ContactThreadSummaryUiModel(
                        id = "contact-hq",
                        title = "運営本部",
                        target = ContactTargetUiModel("headquarters", ContactTargetType.HEADQUARTERS, "運営本部", "報告・確認窓口"),
                        lastMessagePreview = "混雑報告があればこのスレッドへ送ってください。",
                    ),
                    ContactThreadSummaryUiModel(
                        id = "contact-former-a",
                        title = "前Aゲート担当",
                        target = ContactTargetUiModel("former-a", ContactTargetType.PLACE, "前Aゲート担当", "旧担当"),
                        lastMessagePreview = "14:20時点では列が右側に伸びていました。",
                        isFormerAssignment = true,
                    )
                ),
                availableTargets = listOf(
                    ContactTargetUiModel("place-gate-a", ContactTargetType.PLACE, "Aゲート担当", "現担当 3名"),
                    ContactTargetUiModel("role-patrol", ContactTargetType.ROLE, "巡回担当", "会場内巡回"),
                    ContactTargetUiModel("headquarters", ContactTargetType.HEADQUARTERS, "運営本部", "報告・確認窓口"),
                    ContactTargetUiModel("user-sato", ContactTargetType.USER, "佐藤", "個人連絡"),
                ),
                formerAssignments = listOf(
                    FormerAssignmentUiModel(
                        id = "former-a",
                        name = "前Aゲート担当",
                        summary = "旧担当として閲覧・補足返信が可能です。",
                    )
                )
            )

        fun primaryInstructionDetail(currentStaff: CurrentStaffUiModel): InstructionDetailUiModel =
            InstructionDetailUiModel(
                id = "instruction-gate-a",
                title = "Aゲート前の列を右側へ誘導",
                body = "来場者導線を確保し、横断歩道側へ列がはみ出さないよう右側へ寄せてください。",
                target = ContactTargetUiModel("place-gate-a", ContactTargetType.PLACE, "Aゲート担当", "現担当 3名"),
                priorityLabel = "高",
                statusLabel = "対応中",
                locationLabel = "西ホール 入口ゲート A",
                attachmentSummary = "添付画像 1件 / 位置情報あり",
                participants = listOf(
                    InstructionParticipantStatusUiModel(
                        staffName = currentStaff.displayName,
                        statusLabel = "対応中",
                        isCurrentStaff = true,
                    ),
                    InstructionParticipantStatusUiModel("佐藤", "了解"),
                    InstructionParticipantStatusUiModel("山田", "前担当", isFormerStaff = true),
                ),
                thread = listOf(
                    ThreadMessageUiModel(
                        id = "instruction-thread-1",
                        senderName = "運営本部",
                        senderRoleLabel = "HEADQUARTERS",
                        body = "列が伸びてきたので、右側へ寄せながら最後尾を案内してください。",
                        timeLabel = "10:12",
                    ),
                    ThreadMessageUiModel(
                        id = "instruction-thread-2",
                        senderName = currentStaff.displayName,
                        senderRoleLabel = currentStaff.roleLabel ?: "Aゲート担当",
                        body = "対応を開始しました。カラーコーン追加後に再度報告します。",
                        timeLabel = "10:17",
                        isCurrentUser = true,
                    ),
                    ThreadMessageUiModel(
                        id = "instruction-thread-3",
                        senderName = "山田",
                        senderRoleLabel = "前Aゲート担当",
                        body = "14:20時点では列が右側に伸びていました。",
                        timeLabel = "10:18",
                    )
                )
            )

        fun secondaryInstructionDetail(): InstructionDetailUiModel =
            InstructionDetailUiModel(
                id = "instruction-patrol",
                title = "巡回スタッフは西ホール通路の滞留を確認",
                body = "通路の滞留が増えた場合は本部へ即時報告し、立ち止まり客へ移動を促してください。",
                target = ContactTargetUiModel("role-patrol", ContactTargetType.ROLE, "巡回担当", "現担当 2名"),
                priorityLabel = "通常",
                statusLabel = "未確認",
                locationLabel = "西ホール 中央通路",
                attachmentSummary = "位置情報あり",
                participants = listOf(
                    InstructionParticipantStatusUiModel("高橋", "未確認"),
                    InstructionParticipantStatusUiModel("伊藤", "未確認"),
                ),
                thread = emptyList()
            )

        fun InstructionProgressStatus.toStatusLabel(): String =
            when (this) {
                InstructionProgressStatus.UNCONFIRMED -> "未確認"
                InstructionProgressStatus.ACKNOWLEDGED -> "了解"
                InstructionProgressStatus.IN_PROGRESS -> "対応中"
                InstructionProgressStatus.COMPLETED -> "完了"
            }

        fun mergeContactSummary(
            current: List<ContactThreadSummaryUiModel>,
            threadId: String,
            title: String,
            target: ContactTargetUiModel,
            preview: String,
            isFormerAssignment: Boolean,
        ): List<ContactThreadSummaryUiModel> {
            val updated = ContactThreadSummaryUiModel(
                id = threadId,
                title = title,
                target = target,
                lastMessagePreview = preview,
                unreadCount = 0,
                isFormerAssignment = isFormerAssignment,
            )
            return if (current.any { it.id == threadId }) {
                current.map { if (it.id == threadId) updated else it }
            } else {
                listOf(updated) + current
            }
        }

        fun CurrentStaffMember.toUiModel(): CurrentStaffUiModel =
            CurrentStaffUiModel(
                staffId = staffId,
                displayName = displayName,
                roleLabel = roleLabel,
            )
    }
}
