package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.feature.contactchat.ChatThreadTarget
import dev.usbharu.tolo_staff.feature.contactchat.ChatThreadTargetKind
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatService
import dev.usbharu.tolo_staff.feature.contactchat.NoOpContactChatService
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.CurrentStaffSession
import dev.usbharu.tolo_staff.streaming.MockCurrentStaffSession
import dev.usbharu.tolo_staff.streaming.NoOpOperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.OperationAssignment
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepository
import dev.usbharu.tolo_staff.streaming.OperationsOverviewRepositoryImpl
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
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
    private val overviewRepository: OperationsOverviewRepository = OperationsOverviewRepositoryImpl(NoOpOperationsStreamDataSource()),
    private val dataSource: OperationsStreamDataSource = NoOpOperationsStreamDataSource(),
    private val contactChatService: ContactChatService = NoOpContactChatService(),
    private val currentStaffSession: CurrentStaffSession = MockCurrentStaffSession(),
    private val reportRepository: ReportRepository = NoOpReportRepository(),
    private val assignmentStatusService: AssignmentStatusService = NoOpAssignmentStatusService(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : StateEffectViewModel<AppShellUiState, Unit>(AppShellUiState(), coroutineContext) {
    private val logger = AppLogger.withTag("AppShellViewModel")
    private var sessionJob: Job? = null
    private var overviewJob: Job? = null
    private var featureJob: Job? = null
    private var reportJob: Job? = null
    private var observedStaffId: String? = null
    private var instructionDetails = emptyMap<String, InstructionDetailUiModel>()
    private var contactDetails = emptyMap<String, ContactThreadDetailUiModel>()
    private var points = emptyList<OperationPoint>()
    private var staff = emptyList<OperationStaff>()
    private var assignments = emptyList<OperationAssignment>()

    init {
        sessionJob = combine(currentStaffSession.currentStaff, currentStaffSession.isReady) { current, ready -> current to ready }
            .onEach { (current, ready) ->
                updateState {
                    it.copy(
                        currentStaff = CurrentStaffUiModel(current.staffId, current.displayName),
                        isLoading = !ready,
                    )
                }
                if (ready && observedStaffId != current.staffId) {
                    observedStaffId = current.staffId
                    observeData(current.staffId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: AppTab) = updateState { it.copy(selectedTab = tab) }

    fun onHomeInstructionSelected() {
        currentState.homeOverview.currentInstructionId?.let(::onInstructionSelected)
        updateState { it.copy(selectedTab = AppTab.INSTRUCTIONS) }
    }

    fun onPlacementArrivalConfirmed() {
        val placement = currentState.homeOverview.placementStatus
        val id = placement.assignId ?: return
        viewModelScope.launch {
            runCatching { assignmentStatusService.activate(id, placement.expectedVersion) }
                .onSuccess {
                    updateState { state ->
                        state.copy(
                            homeOverview = state.homeOverview.copy(
                                placementStatus = placementStatusFor(
                                    id,
                                    placement.expectedVersion + 1,
                                    placement.placementName,
                                    PlacementPhase.ACTIVE,
                                )
                            ),
                            errorMessage = null,
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun onInstructionSelected(instructionId: String) {
        val detail = instructionDetails[instructionId] ?: return
        updateState { it.copy(instructionsTab = it.instructionsTab.copy(selectedInstruction = detail)) }
    }

    fun onInstructionDetailClosed() = updateState {
        it.copy(instructionsTab = it.instructionsTab.copy(selectedInstruction = null))
    }

    fun onReportTypeSelected(typeId: String) {
        val type = currentState.reportsTab.reportTypes.firstOrNull { it.id == typeId } ?: return
        updateState {
            it.copy(
                reportsTab = it.reportsTab.copy(
                    step = ReportFlowStep.DRAFT_INPUT,
                    draft = ReportDraftUiModel(selectedTypeId = type.id, templateText = type.title),
                )
            )
        }
    }

    fun onReportCommentChanged(comment: String) = updateState {
        it.copy(reportsTab = it.reportsTab.copy(draft = it.reportsTab.draft.copy(comment = comment)))
    }

    fun onReportUrgencySelected(label: String) = updateState {
        it.copy(reportsTab = it.reportsTab.copy(draft = it.reportsTab.draft.copy(urgencyLabel = label)))
    }

    fun onReportContinueToPlaceSelection() = updateState {
        it.copy(reportsTab = it.reportsTab.copy(step = ReportFlowStep.PLACE_SELECTION))
    }

    fun onReportPlaceSelected(placeId: String) {
        val place = currentState.reportsTab.availablePlaces.firstOrNull { it.id == placeId } ?: return
        updateState {
            it.copy(reportsTab = it.reportsTab.copy(draft = it.reportsTab.draft.copy(
                selectedPlaceId = place.id,
                selectedPlaceName = place.displayName,
            )))
        }
    }

    fun onReportSubmitted() {
        val reportState = currentState.reportsTab
        val type = reportState.reportTypes.firstOrNull { it.id == reportState.draft.selectedTypeId } ?: return
        val pointId = reportState.draft.selectedPlaceId ?: return
        val description = listOf(type.title, reportState.draft.comment.trim())
            .filter(String::isNotBlank)
            .joinToString(": ")
        updateState { it.copy(reportsTab = it.reportsTab.copy(isLoadingReports = true, reportsErrorMessage = null)) }
        viewModelScope.launch {
            runCatching {
                reportRepository.submit(type.id.toReportCategory(), reportState.draft.urgencyLabel.toReportSeverity(), pointId, description)
            }.onSuccess { report ->
                val ui = report.toUiModel(currentState.currentStaff.staffId, points, staff)
                updateState { state ->
                    state.copy(
                        reportsTab = state.reportsTab.copy(
                            step = ReportFlowStep.TYPE_SELECTION,
                            draft = ReportDraftUiModel(),
                            relatedReports = listOf(ui) + state.reportsTab.relatedReports.filterNot { it.reportId == ui.reportId },
                            selectedReport = null,
                            isLoadingReports = false,
                        ),
                        homeOverview = state.homeOverview.copy(pendingReportLabel = "最新報告: ${ui.title}"),
                    )
                }
            }.onFailure { error ->
                updateState { it.copy(reportsTab = it.reportsTab.copy(isLoadingReports = false, reportsErrorMessage = error.message)) }
            }
        }
    }

    fun onReportSelected(reportId: String) {
        val report = currentState.reportsTab.relatedReports.firstOrNull { it.reportId == reportId } ?: return
        updateState { it.copy(reportsTab = it.reportsTab.copy(selectedReport = report.toDetail())) }
    }

    fun onReportDetailClosed() = updateState { it.copy(reportsTab = it.reportsTab.copy(selectedReport = null)) }

    fun onReportBack() = updateState { state ->
        val reports = state.reportsTab
        state.copy(reportsTab = when {
            reports.selectedReport != null -> reports.copy(selectedReport = null)
            reports.step == ReportFlowStep.PLACE_SELECTION -> reports.copy(step = ReportFlowStep.DRAFT_INPUT)
            else -> reports.copy(step = ReportFlowStep.TYPE_SELECTION, draft = ReportDraftUiModel())
        })
    }

    fun onContactThreadSelected(threadId: String) {
        val detail = contactDetails[threadId] ?: return
        updateState { it.copy(contactsTab = it.contactsTab.copy(selectedThread = detail, isChoosingTargetType = false)) }
    }

    fun onContactBackToList() = updateState {
        it.copy(contactsTab = it.contactsTab.copy(selectedThread = null, isChoosingTargetType = false, selectedTargetType = null))
    }

    fun onContactNewThreadStarted() = updateState {
        it.copy(
            contactsTab = it.contactsTab.copy(
                selectedThread = null,
                isChoosingTargetType = true,
                selectedTargetType = ContactTargetType.PLACE,
            )
        )
    }

    fun onContactTargetTypeSelected(type: ContactTargetType) = updateState {
        it.copy(contactsTab = it.contactsTab.copy(selectedTargetType = type))
    }

    fun onContactTargetSelected(targetId: String) {
        val target = currentState.contactsTab.availableTargets.firstOrNull { it.id == targetId } ?: return
        viewModelScope.launch {
            runCatching {
                contactChatService.createThread(
                    ChatThreadTarget(
                        target.id,
                        target.displayName,
                        if (target.type == ContactTargetType.PLACE) ChatThreadTargetKind.LOCATION else ChatThreadTargetKind.DIRECT,
                    )
                )
            }.onSuccess { room ->
                val detail = ContactThreadDetailUiModel(room.id, room.title, target)
                contactDetails = contactDetails + (room.id to detail)
                updateState { state ->
                    state.copy(contactsTab = state.contactsTab.copy(
                        threads = listOf(ContactThreadSummaryUiModel(room.id, room.title, target, "")) + state.contactsTab.threads,
                        selectedThread = detail,
                        isChoosingTargetType = false,
                        selectedTargetType = null,
                    ))
                }
            }.onFailure(::showError)
        }
    }

    fun onContactDraftChanged(text: String) {
        val selected = currentState.contactsTab.selectedThread ?: return
        updateState { it.copy(contactsTab = it.contactsTab.copy(selectedThread = selected.copy(draftMessage = text))) }
    }

    fun onContactSendClicked() {
        val selected = currentState.contactsTab.selectedThread ?: return
        val body = selected.draftMessage.trim()
        if (body.isEmpty() || !selected.canReply) return
        updateState { it.copy(contactsTab = it.contactsTab.copy(selectedThread = selected.copy(draftMessage = ""))) }
        viewModelScope.launch {
            runCatching { contactChatService.sendMessage(selected.id, body) }
                .onFailure { error ->
                    updateState { state -> state.copy(
                        contactsTab = state.contactsTab.copy(selectedThread = state.contactsTab.selectedThread?.copy(draftMessage = body)),
                        errorMessage = error.message,
                    ) }
                }
        }
    }

    private fun observeData(staffId: String) {
        overviewJob?.cancel()
        featureJob?.cancel()
        overviewJob = overviewRepository.observeOverview(staffId)
            .onEach { projection -> updateState {
                it.copy(homeOverview = projection.homeOverview.copy(unreadContactCount = it.contactsTab.threads.sumOf { t -> t.unreadCount }), currentPlacementName = projection.currentPlacementName)
            } }
            .catch { showError(it) }
            .launchIn(viewModelScope)

        featureJob = combine(
            dataSource.observePoints(),
            dataSource.observeStaff(),
            dataSource.observeAssignments(),
            dataSource.observeInstructions(),
            dataSource.observeThreads(),
            dataSource.observeMessages(staffId),
        ) { values -> values }
            .onEach { values ->
                @Suppress("UNCHECKED_CAST")
                points = values[0] as List<OperationPoint>
                @Suppress("UNCHECKED_CAST")
                staff = values[1] as List<OperationStaff>
                @Suppress("UNCHECKED_CAST")
                assignments = values[2] as List<OperationAssignment>
                @Suppress("UNCHECKED_CAST")
                val instructions = values[3] as List<dev.usbharu.tolo_staff.streaming.OperationInstruction>
                @Suppress("UNCHECKED_CAST")
                val threads = values[4] as List<dev.usbharu.tolo_staff.streaming.OperationThread>
                @Suppress("UNCHECKED_CAST")
                val messages = values[5] as List<dev.usbharu.tolo_staff.streaming.OperationMessage>
                val instructionProjection = buildInstructionUiProjection(staffId, points, assignments, instructions)
                val contactProjection = buildContactUiProjection(staffId, points, staff, threads, messages)
                instructionDetails = instructionProjection.details
                contactDetails = contactProjection.details
                updateState { state -> state.copy(
                    instructionsTab = instructionProjection.state.copy(
                        selectedInstruction = state.instructionsTab.selectedInstruction?.id?.let(instructionDetails::get)
                    ),
                    contactsTab = contactProjection.state.copy(
                        selectedThread = state.contactsTab.selectedThread?.id?.let(contactDetails::get),
                        isChoosingTargetType = state.contactsTab.isChoosingTargetType,
                        selectedTargetType = state.contactsTab.selectedTargetType,
                    ),
                    reportsTab = state.reportsTab.copy(availablePlaces = points.map {
                        ContactTargetUiModel(it.pointId, ContactTargetType.PLACE, it.name)
                    }),
                    isLoading = false,
                    errorMessage = null,
                ) }
                observeReports(staffId)
            }
            .catch { showError(it) }
            .launchIn(viewModelScope)
    }

    private fun observeReports(staffId: String) {
        val assignedPoints = assignments.assignedPointIds(staffId)
        reportJob?.cancel()
        reportJob = reportRepository.observeRelevantReports(staffId, assignedPoints)
            .onEach { reports -> updateState { state -> state.copy(
                reportsTab = state.reportsTab.copy(
                    relatedReports = reports.map { it.toUiModel(staffId, points, staff) },
                    isLoadingReports = false,
                    reportsErrorMessage = null,
                )
            ) } }
            .catch { error -> updateState { it.copy(reportsTab = it.reportsTab.copy(isLoadingReports = false, reportsErrorMessage = error.message)) } }
            .launchIn(viewModelScope)
    }

    private fun showError(error: Throwable) {
        logger.warn(error) { "App shell operation failed" }
        updateState { it.copy(isLoading = false, errorMessage = error.message ?: "処理に失敗しました") }
    }

    override fun clear() {
        sessionJob?.cancel()
        overviewJob?.cancel()
        featureJob?.cancel()
        reportJob?.cancel()
        super.clear()
    }
}

private fun String.toReportCategory(): ReportCategoryValue = when (this) {
    "congestion" -> ReportCategoryValue.CONGESTION
    "safety_hazard" -> ReportCategoryValue.SAFETY_HAZARD
    "equipment" -> ReportCategoryValue.EQUIPMENT
    "gate" -> ReportCategoryValue.GATE
    else -> ReportCategoryValue.OTHER
}

private fun String.toReportSeverity(): ReportSeverityValue = when (this) {
    "高" -> ReportSeverityValue.ATTENTION
    "緊急" -> ReportSeverityValue.URGENT
    else -> ReportSeverityValue.INFO
}

private fun RelevantReport.toUiModel(
    currentStaffId: String,
    points: List<OperationPoint>,
    staff: List<OperationStaff>,
): RelatedReportUiModel = RelatedReportUiModel(
    reportId = reportId,
    title = category.toLabel(),
    summary = summary,
    severityLabel = severity.toLabel(),
    authorStaffId = authorStaffId,
    authorName = staff.firstOrNull { it.staffId == authorStaffId }?.name ?: authorStaffId,
    targetLabel = points.firstOrNull { it.pointId == pointId }?.name ?: pointId.orEmpty(),
    statusLabel = statusLabel,
    timeLabel = createdAtLabel,
    isAuthoredByCurrentStaff = authorStaffId == currentStaffId,
)

private fun RelatedReportUiModel.toDetail(): ReportDetailUiModel = ReportDetailUiModel(
    reportId = reportId,
    title = title,
    summary = summary,
    severityLabel = severityLabel,
    authorName = authorName,
    targetLabel = targetLabel,
    statusLabel = statusLabel,
    timeLabel = timeLabel,
    isAuthoredByCurrentStaff = isAuthoredByCurrentStaff,
)
