package dev.usbharu.tolo_staff.feature.appshell

enum class ContactTargetType { PLACE, USER }

enum class ReportFlowStep { TYPE_SELECTION, DRAFT_INPUT, PLACE_SELECTION }

data class ContactTargetUiModel(
    val id: String,
    val type: ContactTargetType,
    val displayName: String,
)

data class CurrentStaffUiModel(
    val staffId: String,
    val displayName: String,
)

data class ThreadMessageUiModel(
    val id: String,
    val senderName: String,
    val body: String,
    val timeLabel: String? = null,
    val isCurrentUser: Boolean = false,
)

data class InstructionSummaryUiModel(
    val id: String,
    val title: String,
    val targetName: String,
    val statusLabel: String,
    val preview: String,
)

data class InstructionDetailUiModel(
    val id: String,
    val title: String,
    val body: String,
    val target: ContactTargetUiModel,
    val statusLabel: String,
)

data class InstructionsTabUiState(
    val instructions: List<InstructionSummaryUiModel> = emptyList(),
    val featuredInstruction: InstructionSummaryUiModel? = null,
    val otherInstructions: List<InstructionSummaryUiModel> = emptyList(),
    val selectedInstruction: InstructionDetailUiModel? = null,
)

data class ReportTypeUiModel(
    val id: String,
    val title: String,
    val detailText: String,
)

data class ReportDraftUiModel(
    val selectedTypeId: String? = null,
    val templateText: String = "",
    val comment: String = "",
    val selectedPlaceId: String? = null,
    val selectedPlaceName: String? = null,
    val urgencyLabel: String = "通常",
)

data class RelatedReportUiModel(
    val reportId: String,
    val title: String,
    val summary: String,
    val severityLabel: String,
    val authorStaffId: String,
    val authorName: String,
    val targetLabel: String,
    val statusLabel: String,
    val timeLabel: String? = null,
    val isAuthoredByCurrentStaff: Boolean = false,
)

data class ReportDetailUiModel(
    val reportId: String,
    val title: String,
    val summary: String,
    val severityLabel: String,
    val authorName: String,
    val targetLabel: String,
    val statusLabel: String,
    val timeLabel: String? = null,
    val isAuthoredByCurrentStaff: Boolean = false,
)

data class ReportsTabUiState(
    val reportTypes: List<ReportTypeUiModel> = defaultReportTypes(),
    val availablePlaces: List<ContactTargetUiModel> = emptyList(),
    val draft: ReportDraftUiModel = ReportDraftUiModel(),
    val step: ReportFlowStep = ReportFlowStep.TYPE_SELECTION,
    val relatedReports: List<RelatedReportUiModel> = emptyList(),
    val selectedReport: ReportDetailUiModel? = null,
    val isLoadingReports: Boolean = false,
    val reportsErrorMessage: String? = null,
)

data class ContactThreadSummaryUiModel(
    val id: String,
    val title: String,
    val target: ContactTargetUiModel,
    val lastMessagePreview: String,
    val unreadCount: Int = 0,
)

data class ContactThreadDetailUiModel(
    val id: String,
    val title: String,
    val target: ContactTargetUiModel,
    val messages: List<ThreadMessageUiModel> = emptyList(),
    val draftMessage: String = "",
    val canReply: Boolean = true,
)

data class ContactsTabUiState(
    val threads: List<ContactThreadSummaryUiModel> = emptyList(),
    val selectedThread: ContactThreadDetailUiModel? = null,
    val availableTargets: List<ContactTargetUiModel> = emptyList(),
    val selectedTargetType: ContactTargetType? = null,
    val isChoosingTargetType: Boolean = false,
)

fun defaultReportTypes(): List<ReportTypeUiModel> = listOf(
    ReportTypeUiModel("congestion", "混雑", "入場列や人の流れ、混雑状況を共有します。"),
    ReportTypeUiModel("safety_hazard", "安全上の危険", "事故や安全上の危険を共有します。"),
    ReportTypeUiModel("equipment", "設備", "設備の故障や異常を共有します。"),
    ReportTypeUiModel("gate", "ゲート", "入退場ゲートの状況を共有します。"),
    ReportTypeUiModel("other", "その他", "その他の現場情報を共有します。"),
)
