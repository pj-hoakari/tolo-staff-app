package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.NoOpContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.deriveTitle
import dev.usbharu.tolo_staff.feature.contactchat.sortedByLatestMessage
import dev.usbharu.tolo_staff.feature.contactchat.toPreviewBody
import dev.usbharu.tolo_staff.feature.contactchat.toUiMessage
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.CurrentStaffMember
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.OperationAssignment
import dev.usbharu.tolo_staff.streaming.OperationInstruction
import dev.usbharu.tolo_staff.streaming.OperationInstructionStatus
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.isUnknown
import dev.usbharu.tolo_staff.streaming.relevantTo
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import dev.usbharu.tolo_staff.viewmodel.StateEffectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class AppShellViewModel(
    private val overviewRepository: OperationsOverviewRepository = OperationsOverviewRepositoryImpl(
        dataSource = NoOpOperationsStreamDataSource()
    ),
    private val dataSource: OperationsStreamDataSource = NoOpOperationsStreamDataSource(),
    private val contactChatService: ContactChatService = NoOpContactChatService(),
    private val currentStaffSession: CurrentStaffSession = MockCurrentStaffSession(),
    private val reportRepository: ReportRepository = NoOpReportRepository(),
    coroutineContext: CoroutineContext = Dispatchers.Default
) : StateEffectViewModel<AppShellUiState, Unit>(
    initialState = initialState(
        currentStaff = currentStaffSession.currentStaffSnapshot,
        availableStaff = currentStaffSession.availableStaff.value,
    ),
    coroutineContext = coroutineContext
) {
    private val logger = AppLogger.withTag("AppShellViewModel")
    private var isCurrentStaffReady: Boolean = currentStaffSession.isReady.value
    private var overviewJob: Job? = null
    private var staffSessionJob: Job? = null
    private var reportLoadJob: Job? = null
    private var observedStaffId: String? = null
    private var instructionDetailsById: Map<String, InstructionDetailUiModel> = emptyMap()
    private var contactDetailsById: Map<String, ContactThreadDetailUiModel> = emptyMap()
    private var instructionThreadIdsByInstructionId: Map<String, String> = emptyMap()
    private var localCreatedReports: List<RelatedReportUiModel> = emptyList()
    private var localCreatedContactThreads: Map<String, ContactThreadDetailUiModel> = emptyMap()

    init {
        staffSessionJob = combine(
            currentStaffSession.currentStaff,
            currentStaffSession.availableStaff,
            currentStaffSession.isReady,
        ) { currentStaff, availableStaff, isReady ->
            Triple(currentStaff, availableStaff, isReady)
        }
            .onEach { (currentStaff, availableStaff, isReady) ->
                logger.debug {
                    "Current staff session updated: staffId=${currentStaff.staffId}, availableStaff=${availableStaff.size}, isReady=$isReady"
                }
                val readinessChanged = isCurrentStaffReady != isReady
                isCurrentStaffReady = isReady
                val didChange = observedStaffId != currentStaff.staffId
                observedStaffId = currentStaff.staffId

                updateState { state ->
                    if (!isReady) {
                        state.copy(
                            currentStaff = currentStaff.toUiModel(),
                            availableStaff = availableStaff.map { it.toUiModel() },
                            isLoading = true,
                            errorMessage = null,
                        )
                    } else if (currentStaff.isUnknown()) {
                        initialState(
                            currentStaff = currentStaff,
                            availableStaff = availableStaff,
                        ).copy(
                            selectedTab = state.selectedTab,
                            isLoading = false,
                            errorMessage = "スタッフ情報を取得できませんでした",
                        )
                    } else if (didChange || readinessChanged) {
                        initialState(
                            currentStaff = currentStaff,
                            availableStaff = availableStaff,
                        ).copy(
                            selectedTab = state.selectedTab,
                            isLoading = true,
                            errorMessage = null,
                        )
                    } else {
                        state.copy(
                            currentStaff = currentStaff.toUiModel(),
                            availableStaff = availableStaff.map { it.toUiModel() },
                            errorMessage = null,
                        )
                    }
                }

                if (!isReady || currentStaff.isUnknown()) {
                    logger.info {
                        "Stopping overview observation: isReady=$isReady, staffId=${currentStaff.staffId}, isUnknown=${currentStaff.isUnknown()}"
                    }
                    overviewJob?.cancel()
                    reportLoadJob?.cancel()
                    return@onEach
                }

                if (didChange || readinessChanged) {
                    localCreatedReports = emptyList()
                    localCreatedContactThreads = emptyMap()
                    logger.info {
                        "Refreshing overview observation: staffId=${currentStaff.staffId}, readinessChanged=$readinessChanged"
                    }
                    observeOverview(currentStaff.staffId)
                    loadRelatedReports(currentStaff.staffId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCurrentStaffSelected(staffId: String) {
        logger.info { "Current staff selected from UI: staffId=$staffId" }
        currentStaffSession.selectStaff(staffId)
    }

    fun onTabSelected(tab: AppTab) {
        logger.debug { "Tab selected: tab=$tab" }
        updateState { it.copy(selectedTab = tab) }
    }

    fun onHomeInstructionSelected() {
        val selectedInstructionId = currentState.instructionsTab.selectedInstruction?.id
            ?: currentState.instructionsTab.featuredInstruction?.id
            ?: currentState.instructionsTab.instructions.firstOrNull()?.id
            ?: return
        onInstructionSelected(selectedInstructionId)
        updateState { it.copy(selectedTab = AppTab.INSTRUCTIONS) }
    }

    fun onInstructionSelected(instructionId: String) {
        val detail = instructionDetailsById[instructionId] ?: return
        updateState {
            it.copy(
                instructionsTab = it.instructionsTab.withSelection(
                    selectedInstruction = detail,
                    isShowingThread = false,
                )
            )
        }
    }

    fun onInstructionThreadOpened() {
        val selectedInstructionId = currentState.instructionsTab.selectedInstruction?.id ?: return
        val threadId = instructionThreadIdsByInstructionId[selectedInstructionId] ?: return
        val detail = contactDetailsById[threadId] ?: return
        updateState { state ->
            state.copy(
                selectedTab = AppTab.CONTACTS,
                instructionsTab = state.instructionsTab.copy(isShowingThread = false),
                contactsTab = state.contactsTab.copy(
                    selectedThread = detail,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.INSTRUCTIONS,
                )
            )
        }
    }

    fun onInstructionDetailClosed() {
        updateState {
            it.copy(
                instructionsTab = it.instructionsTab.withSelection(
                    selectedInstruction = null,
                    isShowingThread = false,
                )
            )
        }
    }

    fun onInstructionThreadClosed() {
        updateState {
            it.copy(
                instructionsTab = it.instructionsTab.copy(isShowingThread = false)
            )
        }
    }

    fun onInstructionStatusUpdated(status: InstructionProgressStatus) {
        val selectedInstruction = currentState.instructionsTab.selectedInstruction ?: return
        val nextStatusLabel = status.toLabel()
        updateState { state ->
            val updatedSelectedInstruction = selectedInstruction.copy(statusLabel = nextStatusLabel)
            state.copy(
                homeOverview = if (state.homeOverview.currentInstructionId == selectedInstruction.id) {
                    state.homeOverview.copy(currentInstructionStatusLabel = nextStatusLabel)
                } else {
                    state.homeOverview
                },
                instructionsTab = state.instructionsTab.copy(
                    selectedInstruction = updatedSelectedInstruction,
                    instructions = state.instructionsTab.instructions.map { instruction ->
                        if (instruction.id == selectedInstruction.id) {
                            instruction.copy(statusLabel = nextStatusLabel)
                        } else {
                            instruction
                        }
                    },
                    featuredInstruction = state.instructionsTab.featuredInstruction?.let { instruction ->
                        if (instruction.id == selectedInstruction.id) {
                            instruction.copy(statusLabel = nextStatusLabel)
                        } else {
                            instruction
                        }
                    },
                    otherInstructions = state.instructionsTab.otherInstructions.map { instruction ->
                        if (instruction.id == selectedInstruction.id) {
                            instruction.copy(statusLabel = nextStatusLabel)
                        } else {
                            instruction
                        }
                    },
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
        if (currentState.reportsTab.availablePlaces.isEmpty()) {
            return
        }
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
        val reportsTab = currentState.reportsTab
        val selectedType = reportsTab.reportTypes.firstOrNull { it.id == reportsTab.draft.selectedTypeId } ?: return
        val selectedPlace = reportsTab.availablePlaces.firstOrNull { it.id == reportsTab.draft.selectedPlaceId } ?: return
        val submittedSummary = buildString {
            append(selectedType.title)
            if (reportsTab.draft.comment.isNotBlank()) {
                append(": ")
                append(reportsTab.draft.comment)
            }
            if (reportsTab.draft.urgencyLabel.isNotBlank()) {
                append(" [")
                append(reportsTab.draft.urgencyLabel)
                append("]")
            }
        }
        val threadId = "report-${selectedType.id}-${selectedPlace.id}"
        val currentStaff = currentState.currentStaff
        val submittedThread = ContactThreadDetailUiModel(
            id = threadId,
            title = "${selectedType.title} / ${selectedPlace.displayName}",
            target = ContactTargetUiModel(
                id = selectedPlace.id,
                type = ContactTargetType.PLACE,
                displayName = selectedPlace.displayName,
                subtitle = "本部"
            ),
            messages = listOf(
                ThreadMessageUiModel(
                    id = "$threadId-message",
                    senderName = currentStaff.displayName,
                    senderRoleLabel = currentStaff.roleLabel,
                    body = submittedSummary,
                    isCurrentUser = true,
                )
            ),
            canReply = true,
        )
        val submittedReport = RelatedReportUiModel(
            reportId = threadId,
            threadId = threadId,
            title = selectedType.title,
            summary = submittedSummary,
            priorityLabel = reportsTab.draft.urgencyLabel,
            authorStaffId = currentStaff.staffId,
            authorName = currentStaff.displayName,
            targetLabel = selectedPlace.displayName,
            isAuthoredByCurrentStaff = true,
        )
        localCreatedReports = mergeRelatedReports(localCreatedReports, listOf(submittedReport))
        localCreatedContactThreads = localCreatedContactThreads + (threadId to submittedThread)

        updateState { state ->
            state.copy(
                reportsTab = state.reportsTab.copy(
                    relatedReports = mergeRelatedReports(state.reportsTab.relatedReports, localCreatedReports),
                    step = ReportFlowStep.TYPE_SELECTION,
                    draft = ReportDraftUiModel(),
                ),
                selectedTab = AppTab.CONTACTS,
                contactsTab = state.contactsTab.copy(
                    threads = mergeLocalContactThreads(
                        baseThreads = state.contactsTab.threads,
                        localThreads = localCreatedContactThreads,
                    ),
                    selectedThread = submittedThread,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.REPORTS,
                ),
                homeOverview = state.homeOverview.copy(
                    pendingReportLabel = "最新報告: ${selectedType.title}"
                ),
            )
        }
    }

    fun onReportSelected(reportId: String) {
        val report = currentState.reportsTab.relatedReports.firstOrNull { it.reportId == reportId } ?: return
        val detail = contactDetailsById[report.threadId]
            ?: localCreatedContactThreads[report.threadId]
            ?: buildRelatedReportContactThread(report)
        updateState { state ->
            state.copy(
                selectedTab = AppTab.CONTACTS,
                contactsTab = state.contactsTab.copy(
                    threads = mergeLocalContactThreads(
                        baseThreads = state.contactsTab.threads,
                        localThreads = localCreatedContactThreads,
                    ),
                    selectedThread = detail,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.REPORTS,
                )
            )
        }
    }

    fun onReportBack() {
        updateState { state ->
            val reportsTab = state.reportsTab
            val nextStep = when (reportsTab.step) {
                ReportFlowStep.PLACE_SELECTION -> ReportFlowStep.DRAFT_INPUT
                ReportFlowStep.DRAFT_INPUT -> ReportFlowStep.TYPE_SELECTION
                ReportFlowStep.TYPE_SELECTION -> ReportFlowStep.TYPE_SELECTION
            }
            state.copy(
                reportsTab = reportsTab.copy(
                    step = nextStep,
                )
            )
        }
    }

    fun onContactThreadSelected(threadId: String) {
        val detail = contactDetailsById[threadId] ?: localCreatedContactThreads[threadId] ?: return
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    selectedThread = detail,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.NONE,
                )
            )
        }
    }

    fun onContactBackToList() {
        updateState { state ->
            state.copy(
                selectedTab = when (state.contactsTab.selectedThreadBackDestination) {
                    ContactThreadBackDestination.INSTRUCTIONS -> AppTab.INSTRUCTIONS
                    ContactThreadBackDestination.REPORTS -> AppTab.REPORTS
                    ContactThreadBackDestination.NONE -> state.selectedTab
                },
                contactsTab = state.contactsTab.copy(
                    selectedThread = null,
                    isChoosingTargetType = false,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.NONE,
                )
            )
        }
    }

    fun onContactNewThreadStarted() {
        if (currentState.contactsTab.availableTargets.isEmpty()) {
            return
        }
        updateState {
            it.copy(
                contactsTab = it.contactsTab.copy(
                    selectedThread = null,
                    isChoosingTargetType = true,
                    selectedTargetType = null,
                    selectedThreadBackDestination = ContactThreadBackDestination.NONE,
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

    fun onContactTargetSelected(targetId: String) = Unit

    fun onContactDraftChanged(text: String) {
        val selectedThread = currentState.contactsTab.selectedThread ?: return
        if (!selectedThread.canReply) {
            return
        }
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
        if (!selectedThread.canReply) {
            return
        }
        val message = selectedThread.draftMessage.trim()
        if (message.isEmpty()) {
            return
        }
        val threadId = selectedThread.id
        val currentStaffId = currentState.currentStaff.staffId

        updateState { state ->
            state.copy(
                contactsTab = state.contactsTab.copy(
                    selectedThread = selectedThread.copy(draftMessage = "")
                ),
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                contactChatService.sendSimpleMessage(
                    roomId = threadId,
                    currentStaffId = currentStaffId,
                    text = message,
                )
            }.onFailure { throwable ->
                logger.warn(throwable) {
                    "Failed to send contact message: threadId=$threadId, currentStaffId=$currentStaffId"
                }
                updateState { state ->
                    val currentThread = state.contactsTab.selectedThread
                    state.copy(
                        contactsTab = state.contactsTab.copy(
                            selectedThread = currentThread
                                ?.takeIf { it.id == threadId }
                                ?.copy(draftMessage = message)
                                ?: currentThread
                        ),
                        errorMessage = throwable.message ?: "メッセージ送信に失敗しました",
                    )
                }
            }
        }
    }

    override fun clear() {
        logger.trace { "Clearing AppShellViewModel subscriptions" }
        overviewJob?.cancel()
        staffSessionJob?.cancel()
        reportLoadJob?.cancel()
        super.clear()
    }

    private fun loadRelatedReports(currentStaffId: String) {
        reportLoadJob?.cancel()
        reportLoadJob = viewModelScope.launch {
            updateState { state ->
                state.copy(
                    reportsTab = state.reportsTab.copy(
                        isLoadingReports = true,
                        reportsErrorMessage = null,
                    )
                )
            }

            runCatching { reportRepository.listRelevantReports(currentStaffId) }
                .onSuccess { reports ->
                    updateState { state ->
                        val remoteReports = reports
                            .sortedByDescending { it.createdAtLabel.orEmpty() }
                            .map { report ->
                                report.toUiModel(
                                    currentStaffId = currentStaffId,
                                    currentThreadTitle = contactDetailsById[report.threadId]?.title,
                                    availableStaff = state.availableStaff,
                                )
                            }
                        val mergedReports = mergeRelatedReports(remoteReports, localCreatedReports)
                        state.copy(
                            reportsTab = state.reportsTab.copy(
                                relatedReports = mergedReports,
                                isLoadingReports = false,
                                reportsErrorMessage = null,
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    logger.warn(throwable) {
                        "Failed to load related reports: currentStaffId=$currentStaffId"
                    }
                    updateState { state ->
                        state.copy(
                            reportsTab = state.reportsTab.copy(
                                isLoadingReports = false,
                                reportsErrorMessage = throwable.message ?: "関連報告の取得に失敗しました",
                            )
                        )
                    }
                }
        }
    }

    private fun observeOverview(currentStaffId: String) {
        overviewJob?.cancel()
        dataSource.start()
        logger.info { "observeOverview subscribed: currentStaffId=$currentStaffId" }
        val combinedStream = combine(
            overviewRepository.observeOverview(currentStaffId),
            dataSource.observePoints(),
            dataSource.observeStaff(),
            dataSource.observeAssignments(),
            dataSource.observeInstructions(),
            dataSource.observeThreads(),
            dataSource.observeMessages(currentStaffId),
        ) { values ->
            values
        }
        overviewJob = combinedStream
            .onEach { values ->
                val projection = values[0] as dev.usbharu.tolo_staff.streaming.AppShellOperationsProjection
                val points = values[1] as List<OperationPoint>
                val staff = values[2] as List<OperationStaff>
                val assignments = values[3] as List<OperationAssignment>
                val instructions = values[4] as List<OperationInstruction>
                val threads = values[5] as List<OperationThread>
                val messages = values[6] as List<OperationMessage>
                logger.debug {
                    "Received overview snapshot: currentStaffId=$currentStaffId, points=${points.size}, staff=${staff.size}, assignments=${assignments.size}, instructions=${instructions.size}, threads=${threads.size}, messages=${messages.size}"
                }
                val snapshot = buildSnapshot(
                    currentStaffId = currentStaffId,
                    currentStaff = currentState.currentStaff,
                    projection = projection.homeOverview to projection.currentPlacementName,
                    points = points,
                    staff = staff,
                    assignments = assignments,
                    instructions = instructions,
                    threads = threads,
                    messages = messages,
                )
                instructionDetailsById = snapshot.instructionDetailsById
                contactDetailsById = snapshot.contactDetailsById + localCreatedContactThreads
                instructionThreadIdsByInstructionId = snapshot.instructionThreadIdsByInstructionId

                updateState { state ->
                    val selectedInstruction = state.instructionsTab.selectedInstruction
                        ?.id
                        ?.let(snapshot.instructionDetailsById::get)
                    val previousSelectedThread = state.contactsTab.selectedThread
                    val selectedThread = previousSelectedThread
                        ?.id
                        ?.let { threadId -> (snapshot.contactDetailsById + localCreatedContactThreads)[threadId] }
                        ?.copy(draftMessage = previousSelectedThread.draftMessage)
                    val selectedTargetType = state.contactsTab.selectedTargetType?.takeIf { type ->
                        snapshot.contactsTab.availableTargets.any { it.type == type }
                    }
                    val resolvedReports = state.reportsTab.relatedReports.map { report ->
                        report.withResolvedMetadata(
                            currentStaffId = currentStaffId,
                            currentThreadTitle = snapshot.contactDetailsById[report.threadId]?.title,
                            availableStaff = staff,
                        )
                    }
                    state.copy(
                        homeOverview = snapshot.homeOverview,
                        currentPlacementName = snapshot.currentPlacementName,
                        instructionsTab = snapshot.instructionsTab.withSelection(
                            selectedInstruction = selectedInstruction,
                            isShowingThread = state.instructionsTab.isShowingThread && selectedInstruction != null,
                        ),
                        reportsTab = state.reportsTab.copy(
                            reportTypes = snapshot.reportTypes,
                            availablePlaces = snapshot.reportPlaces,
                            relatedReports = resolvedReports,
                            draft = state.reportsTab.draft.copy(
                                selectedPlaceId = state.reportsTab.draft.selectedPlaceId?.takeIf { placeId ->
                                    snapshot.reportPlaces.any { it.id == placeId }
                                },
                                selectedPlaceName = state.reportsTab.draft.selectedPlaceName?.takeIf { placeName ->
                                    snapshot.reportPlaces.any { it.displayName == placeName }
                                },
                            ),
                        ),
                        contactsTab = snapshot.contactsTab.copy(
                            threads = mergeLocalContactThreads(
                                baseThreads = snapshot.contactsTab.threads,
                                localThreads = localCreatedContactThreads,
                            ),
                            selectedThread = selectedThread,
                            selectedTargetType = selectedTargetType,
                            isChoosingTargetType = state.contactsTab.isChoosingTargetType && snapshot.contactsTab.availableTargets.isNotEmpty(),
                            selectedThreadBackDestination = if (selectedThread != null) {
                                state.contactsTab.selectedThreadBackDestination
                            } else {
                                ContactThreadBackDestination.NONE
                            },
                        ),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            .catch { throwable ->
                logger.warn(throwable) { "Failed to observe overview: currentStaffId=$currentStaffId" }
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "ホーム情報の購読に失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun buildSnapshot(
        currentStaffId: String,
        currentStaff: CurrentStaffUiModel,
        projection: Pair<AppShellHomeOverview, String>,
        points: List<OperationPoint>,
        staff: List<OperationStaff>,
        assignments: List<OperationAssignment>,
        instructions: List<OperationInstruction>,
        threads: List<OperationThread>,
        messages: List<OperationMessage>,
    ): AppShellSnapshot {
        val relevantInstructions = instructions.relevantTo(
            currentStaffId = currentStaffId,
            assignments = assignments,
        )
        val instructionModels = buildInstructionModels(
            currentStaffId = currentStaffId,
            currentStaff = currentStaff,
            points = points,
            staff = staff,
            assignments = assignments,
            instructions = relevantInstructions,
            threads = threads,
            messages = messages,
        )
        val contactModels = buildContactModels(
            currentStaffId = currentStaffId,
            staff = staff,
            assignments = assignments,
            points = points,
            threads = threads,
            messages = messages,
        )
        val homeOverview = projection.first.copy(
            currentInstructionUnreadCount = 0,
            unreadContactCount = contactModels.contactsTab.threads.sumOf { it.unreadCount },
        )
        val instructionSummaries = instructionModels.instructionsTab.instructions.ifEmpty {
            homeOverview.toFallbackInstructionSummary()?.let(::listOf).orEmpty()
        }
        return AppShellSnapshot(
            homeOverview = homeOverview,
            currentPlacementName = projection.second,
            instructionsTab = instructionModels.instructionsTab.withInstructions(
                instructions = instructionSummaries,
                featuredInstructionId = homeOverview.currentInstructionId,
            ),
            contactsTab = contactModels.contactsTab,
            reportTypes = defaultReportTypes(),
            reportPlaces = points.map { point ->
                ContactTargetUiModel(
                    id = point.pointId,
                    type = ContactTargetType.PLACE,
                    displayName = point.name,
                    subtitle = point.description.ifBlank { null },
                )
            },
            instructionDetailsById = instructionModels.detailsById,
            contactDetailsById = contactModels.detailsById,
            instructionThreadIdsByInstructionId = instructionModels.threadIdByInstructionId,
        )
    }

    private fun buildInstructionModels(
        currentStaffId: String,
        currentStaff: CurrentStaffUiModel,
        points: List<OperationPoint>,
        staff: List<OperationStaff>,
        assignments: List<OperationAssignment>,
        instructions: List<OperationInstruction>,
        threads: List<OperationThread>,
        messages: List<OperationMessage>,
    ): BuiltInstructionModels {
        val pointsById = points.associateBy { it.pointId }
        val staffById = staff.associateBy { it.staffId }
        val messagesByThreadId = messages.groupBy { it.threadId }
        val threadIdByInstructionId = messages
            .filter { it.instructionId != null }
            .groupBy { it.instructionId.orEmpty() }
            .mapValues { (_, instructionMessages) ->
                instructionMessages
                    .sortedOperationMessages()
                    .lastOrNull()
                    ?.threadId
                    .orEmpty()
            }
            .filterValues { it.isNotBlank() }

        val details = instructions
            .sortedBy { it.instructionId }
            .map { instruction ->
                val target = instruction.toTarget(pointsById, staffById)
                val instructionThreadId = threadIdByInstructionId[instruction.instructionId]
                val threadMessages = instructionThreadId
                    ?.let(messagesByThreadId::get)
                    .orEmpty()
                    .sortedOperationMessages()
                    .map { message -> message.toThreadMessageUiModel(currentStaffId, staff) }
                val participantStaffIds = buildSet {
                    addAll(instruction.staffIds)
                    assignments
                        .filter { it.pointId in instruction.pointIds }
                        .mapTo(this) { it.staffId }
                }
                val participants = participantStaffIds
                    .sorted()
                    .map { staffId ->
                        val displayName = staffById[staffId]?.name ?: staffId
                        InstructionParticipantStatusUiModel(
                            staffName = displayName,
                            statusLabel = instruction.status.toStatusLabel(),
                            isCurrentStaff = staffId == currentStaff.staffId,
                        )
                    }
                InstructionDetailUiModel(
                    id = instruction.instructionId,
                    title = instruction.title,
                    body = instruction.description,
                    target = target,
                    priorityLabel = "",
                    statusLabel = instruction.status.toStatusLabel(),
                    locationLabel = instruction.pointIds
                        .mapNotNull { pointId -> pointsById[pointId]?.description?.ifBlank { null } }
                        .distinct()
                        .joinToString(" / ")
                        .ifBlank { null },
                    attachmentSummary = null,
                    participants = participants,
                    thread = threadMessages,
                )
            }

        val detailsById = details.associateBy { it.id }
        val summaries = details.map { detail ->
            InstructionSummaryUiModel(
                id = detail.id,
                title = detail.title,
                targetName = detail.target.displayName,
                priorityLabel = detail.priorityLabel,
                statusLabel = detail.statusLabel,
                preview = detail.body.ifBlank { detail.title },
                locationLabel = detail.locationLabel,
                attachmentSummary = detail.attachmentSummary,
                unreadCount = 0,
            )
        }
        return BuiltInstructionModels(
            instructionsTab = InstructionsTabUiState(
                instructions = summaries,
                featuredInstruction = null,
                otherInstructions = emptyList(),
                selectedInstruction = null,
                isShowingThread = false,
            ),
            detailsById = detailsById,
            threadIdByInstructionId = threadIdByInstructionId,
        )
    }

    private fun buildContactModels(
        currentStaffId: String,
        staff: List<OperationStaff>,
        assignments: List<OperationAssignment>,
        points: List<OperationPoint>,
        threads: List<OperationThread>,
        messages: List<OperationMessage>,
    ): BuiltContactModels {
        val threadMessagesById = messages.groupBy { it.threadId }
        val visibleThreads = threads
            .filter { currentStaffId in it.members }
            .sortedByLatestMessage(messages)

        val details = visibleThreads.map { thread ->
            val title = thread.deriveTitle(currentStaffId, staff)
            val target = ContactTargetUiModel(
                id = thread.threadId,
                type = if (thread.members.count { it != currentStaffId } <= 1) ContactTargetType.USER else ContactTargetType.ROLE,
                displayName = title,
                subtitle = null,
            )
            ContactThreadDetailUiModel(
                id = thread.threadId,
                title = title,
                target = target,
                messages = threadMessagesById[thread.threadId]
                    .orEmpty()
                    .sortedOperationMessages()
                    .map { message -> message.toThreadMessageUiModel(currentStaffId, staff) },
                canReply = true,
                isFormerAssignment = false,
            )
        }
        val detailsById = details.associateBy { it.id }
        val summaries = details.map { detail ->
            ContactThreadSummaryUiModel(
                id = detail.id,
                title = detail.title,
                target = detail.target,
                lastMessagePreview = detail.messages.lastOrNull()?.body.orEmpty(),
                unreadCount = 0,
                isFormerAssignment = false,
            )
        }
        return BuiltContactModels(
            contactsTab = ContactsTabUiState(
                threads = summaries,
                selectedThread = null,
                availableTargets = emptyList(),
                selectedTargetType = null,
                isChoosingTargetType = false,
                formerAssignments = emptyList(),
                selectedThreadBackDestination = ContactThreadBackDestination.NONE,
            ),
            detailsById = detailsById,
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
                reportsTab = ReportsTabUiState(),
                contactsTab = ContactsTabUiState(),
                instructionsTab = InstructionsTabUiState(),
                isLoading = true,
            )

        fun CurrentStaffMember.toUiModel(): CurrentStaffUiModel =
            CurrentStaffUiModel(
                staffId = staffId,
                displayName = displayName,
                roleLabel = roleLabel,
            )

        fun OperationInstructionStatus.toStatusLabel(): String =
            when (this) {
                OperationInstructionStatus.ACTIVE -> "対応中"
            }

        fun InstructionProgressStatus.toLabel(): String =
            when (this) {
                InstructionProgressStatus.UNCONFIRMED -> "未確認"
                InstructionProgressStatus.ACKNOWLEDGED -> "了解"
                InstructionProgressStatus.IN_PROGRESS -> "対応中"
                InstructionProgressStatus.COMPLETED -> "完了"
            }

        fun defaultReportTypes(): List<ReportTypeUiModel> = listOf(
            ReportTypeUiModel(
                id = "queue",
                title = "導線報告",
                detailText = "入場列や人の流れ、混雑状況を本部へ共有します。",
            ),
            ReportTypeUiModel(
                id = "incident",
                title = "トラブル報告",
                detailText = "来場者対応や設備異常など、即時共有が必要な事項を報告します。",
            ),
            ReportTypeUiModel(
                id = "handover",
                title = "引き継ぎ報告",
                detailText = "次の担当者や本部へ、現場状況と注意点を引き継ぎます。",
            ),
        )

        fun OperationInstruction.toTarget(
            pointsById: Map<String, OperationPoint>,
            staffById: Map<String, OperationStaff>,
        ): ContactTargetUiModel {
            val pointNames = pointIds.mapNotNull { pointId -> pointsById[pointId]?.name }.distinct()
            if (pointNames.isNotEmpty()) {
                return ContactTargetUiModel(
                    id = pointIds.first(),
                    type = ContactTargetType.PLACE,
                    displayName = pointNames.joinToString(" / "),
                    subtitle = pointIds
                        .mapNotNull { pointId -> pointsById[pointId]?.description?.ifBlank { null } }
                        .distinct()
                        .joinToString(" / ")
                        .ifBlank { null },
                )
            }

            val staffNames = staffIds.map { staffId -> staffById[staffId]?.name ?: staffId }.distinct()
            return ContactTargetUiModel(
                id = staffIds.firstOrNull() ?: instructionId,
                type = ContactTargetType.USER,
                displayName = staffNames.joinToString(", ").ifBlank { title },
                subtitle = null,
            )
        }
    }
}

private fun mergeLocalContactThreads(
    baseThreads: List<ContactThreadSummaryUiModel>,
    localThreads: Map<String, ContactThreadDetailUiModel>,
): List<ContactThreadSummaryUiModel> {
    val baseOrder = baseThreads.mapIndexed { index, summary -> summary.id to index }.toMap()
    val merged = baseThreads.associateBy { it.id }.toMutableMap()
    localThreads.values.forEach { detail ->
        merged[detail.id] = ContactThreadSummaryUiModel(
            id = detail.id,
            title = detail.title,
            target = detail.target,
            lastMessagePreview = detail.messages.lastOrNull()?.body.orEmpty(),
            unreadCount = 0,
            isFormerAssignment = detail.isFormerAssignment,
        )
    }
    return merged.values.sortedWith(
        compareBy<ContactThreadSummaryUiModel> { summary ->
            baseOrder[summary.id] ?: Int.MAX_VALUE
        }.thenBy { it.id }
    )
}

private fun buildRelatedReportContactThread(report: RelatedReportUiModel): ContactThreadDetailUiModel {
    val target = ContactTargetUiModel(
        id = report.threadId,
        type = ContactTargetType.HEADQUARTERS,
        displayName = report.targetLabel,
    )
    return ContactThreadDetailUiModel(
        id = report.threadId,
        title = report.title,
        target = target,
        messages = listOf(
            ThreadMessageUiModel(
                id = "${report.reportId}-summary",
                senderName = report.authorName,
                body = report.summary,
                timeLabel = report.timeLabel,
                isCurrentUser = report.isAuthoredByCurrentStaff,
            )
        ),
    )
}

private fun AppShellHomeOverview.toFallbackInstructionSummary(): InstructionSummaryUiModel? {
    if (currentInstruction.isBlank()) {
        return null
    }
    return InstructionSummaryUiModel(
        id = currentInstructionId ?: "home-overview-instruction",
        title = currentInstructionTitle ?: "あなたへの指示",
        targetName = currentInstructionTargetName ?: "",
        priorityLabel = currentInstructionPriorityLabel ?: "",
        statusLabel = currentInstructionStatusLabel ?: "",
        preview = currentInstruction,
        locationLabel = currentInstructionLocationLabel,
        attachmentSummary = currentInstructionAttachmentSummary,
        unreadCount = currentInstructionUnreadCount,
    )
}

private data class AppShellSnapshot(
    val homeOverview: AppShellHomeOverview,
    val currentPlacementName: String,
    val instructionsTab: InstructionsTabUiState,
    val contactsTab: ContactsTabUiState,
    val reportTypes: List<ReportTypeUiModel>,
    val reportPlaces: List<ContactTargetUiModel>,
    val instructionDetailsById: Map<String, InstructionDetailUiModel>,
    val contactDetailsById: Map<String, ContactThreadDetailUiModel>,
    val instructionThreadIdsByInstructionId: Map<String, String>,
)

private data class BuiltInstructionModels(
    val instructionsTab: InstructionsTabUiState,
    val detailsById: Map<String, InstructionDetailUiModel>,
    val threadIdByInstructionId: Map<String, String>,
)

private data class BuiltContactModels(
    val contactsTab: ContactsTabUiState,
    val detailsById: Map<String, ContactThreadDetailUiModel>,
)

private fun RelevantReport.toUiModel(
    currentStaffId: String,
    currentThreadTitle: String?,
    availableStaff: List<CurrentStaffUiModel>,
): RelatedReportUiModel = RelatedReportUiModel(
    reportId = reportId,
    threadId = threadId,
    title = title,
    summary = summary,
    priorityLabel = priorityLabel,
    authorStaffId = authorStaffId,
    authorName = availableStaff.firstOrNull { it.staffId == authorStaffId }?.displayName ?: authorStaffId,
    targetLabel = currentThreadTitle ?: title,
    timeLabel = createdAtLabel,
    isAuthoredByCurrentStaff = authorStaffId == currentStaffId,
)

private fun RelatedReportUiModel.withResolvedMetadata(
    currentStaffId: String,
    currentThreadTitle: String?,
    availableStaff: List<OperationStaff>,
): RelatedReportUiModel = copy(
    authorName = availableStaff.firstOrNull { it.staffId == authorStaffId }?.name ?: authorName,
    targetLabel = currentThreadTitle ?: targetLabel,
    isAuthoredByCurrentStaff = authorStaffId == currentStaffId,
)

private fun mergeRelatedReports(
    remote: List<RelatedReportUiModel>,
    local: List<RelatedReportUiModel>,
): List<RelatedReportUiModel> = (local + remote).distinctBy { it.reportId }

private fun OperationMessage.toThreadMessageUiModel(
    currentStaffId: String,
    staff: List<OperationStaff>,
): ThreadMessageUiModel {
    val chatMessage = toUiMessage(currentStaffId, staff)
    return ThreadMessageUiModel(
        id = chatMessage.id,
        senderName = chatMessage.senderName,
        body = chatMessage.body,
        timeLabel = chatMessage.timeLabel,
        isCurrentUser = chatMessage.isFromCurrentUser,
        isSystemEvent = chatMessage.isSystemEvent,
    )
}

private fun InstructionsTabUiState.withInstructions(
    instructions: List<InstructionSummaryUiModel>,
    featuredInstructionId: String?,
): InstructionsTabUiState {
    val featured = instructions.firstOrNull { it.id == featuredInstructionId } ?: instructions.firstOrNull()
    val featuredId = featured?.id
    return copy(
        instructions = instructions,
        featuredInstruction = featured,
        otherInstructions = instructions.filterNot { it.id == featuredId },
    )
}

private fun InstructionsTabUiState.withSelection(
    selectedInstruction: InstructionDetailUiModel?,
    isShowingThread: Boolean,
): InstructionsTabUiState = copy(
    selectedInstruction = selectedInstruction,
    isShowingThread = isShowingThread,
)
