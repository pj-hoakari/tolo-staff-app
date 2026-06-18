package dev.usbharu.tolo_staff.feature.appshell

enum class ContactTargetType {
    PLACE,
    ROLE,
    HEADQUARTERS,
    USER
}

enum class InstructionProgressStatus {
    UNCONFIRMED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    COMPLETED
}

enum class ReportFlowStep {
    TYPE_SELECTION,
    DRAFT_INPUT,
    PLACE_SELECTION,
    THREAD
}

data class ContactTargetUiModel(
    val id: String,
    val type: ContactTargetType,
    val displayName: String,
    val subtitle: String? = null,
)

data class CurrentStaffUiModel(
    val staffId: String,
    val displayName: String,
    val roleLabel: String? = null,
)

data class ThreadMessageUiModel(
    val id: String,
    val senderName: String,
    val senderRoleLabel: String? = null,
    val body: String,
    val timeLabel: String? = null,
    val isCurrentUser: Boolean = false,
    val isSystemEvent: Boolean = false,
)

data class InstructionParticipantStatusUiModel(
    val staffName: String,
    val statusLabel: String,
    val isCurrentStaff: Boolean = false,
    val isFormerStaff: Boolean = false,
)

data class InstructionSummaryUiModel(
    val id: String,
    val title: String,
    val targetName: String,
    val priorityLabel: String,
    val statusLabel: String,
    val preview: String,
    val locationLabel: String? = null,
    val attachmentSummary: String? = null,
    val unreadCount: Int = 0,
)

data class InstructionDetailUiModel(
    val id: String,
    val title: String,
    val body: String,
    val target: ContactTargetUiModel,
    val priorityLabel: String,
    val statusLabel: String,
    val locationLabel: String? = null,
    val attachmentSummary: String? = null,
    val participants: List<InstructionParticipantStatusUiModel> = emptyList(),
    val thread: List<ThreadMessageUiModel> = emptyList(),
)

data class InstructionsTabUiState(
    val instructions: List<InstructionSummaryUiModel> = emptyList(),
    val featuredInstruction: InstructionSummaryUiModel? = null,
    val otherInstructions: List<InstructionSummaryUiModel> = emptyList(),
    val selectedInstruction: InstructionDetailUiModel? = null,
    val isShowingThread: Boolean = false,
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
    val includesImage: Boolean = false,
    val includesLocation: Boolean = false,
)

data class ReportThreadUiModel(
    val id: String,
    val title: String,
    val targetLabel: String,
    val messages: List<ThreadMessageUiModel> = emptyList(),
    val lastSubmittedSummary: String = "",
)

data class ReportsTabUiState(
    val reportTypes: List<ReportTypeUiModel> = emptyList(),
    val availablePlaces: List<ContactTargetUiModel> = emptyList(),
    val draft: ReportDraftUiModel = ReportDraftUiModel(),
    val step: ReportFlowStep = ReportFlowStep.TYPE_SELECTION,
    val submittedThread: ReportThreadUiModel? = null,
)

data class ContactThreadSummaryUiModel(
    val id: String,
    val title: String,
    val target: ContactTargetUiModel,
    val lastMessagePreview: String,
    val unreadCount: Int = 0,
    val isFormerAssignment: Boolean = false,
)

data class ContactThreadDetailUiModel(
    val id: String,
    val title: String,
    val target: ContactTargetUiModel,
    val messages: List<ThreadMessageUiModel> = emptyList(),
    val draftMessage: String = "",
    val canReply: Boolean = true,
    val isFormerAssignment: Boolean = false,
)

data class FormerAssignmentUiModel(
    val id: String,
    val name: String,
    val summary: String,
    val canReply: Boolean = true,
)

data class ContactsTabUiState(
    val threads: List<ContactThreadSummaryUiModel> = emptyList(),
    val selectedThread: ContactThreadDetailUiModel? = null,
    val availableTargets: List<ContactTargetUiModel> = emptyList(),
    val selectedTargetType: ContactTargetType? = null,
    val isChoosingTargetType: Boolean = false,
    val formerAssignments: List<FormerAssignmentUiModel> = emptyList(),
    val shouldReturnToInstructionOnBack: Boolean = false,
)
