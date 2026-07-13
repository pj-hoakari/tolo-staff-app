package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo_staff.feature.contactchat.deriveTitle
import dev.usbharu.tolo_staff.feature.contactchat.toPreviewBody
import dev.usbharu.tolo_staff.streaming.OperationAssignment
import dev.usbharu.tolo_staff.streaming.OperationAssignmentStatus
import dev.usbharu.tolo_staff.streaming.OperationInstruction
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationPoint
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.OperationThreadKind
import dev.usbharu.tolo_staff.streaming.OperationThreadStatus
import dev.usbharu.tolo_staff.streaming.relevantTo
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import dev.usbharu.tolo_staff.streaming.targetLabel
import dev.usbharu.tolo_staff.streaming.toLabel

internal data class InstructionUiProjection(
    val state: InstructionsTabUiState,
    val details: Map<String, InstructionDetailUiModel>,
)

internal fun buildInstructionUiProjection(
    currentStaffId: String,
    points: List<OperationPoint>,
    assignments: List<OperationAssignment>,
    instructions: List<OperationInstruction>,
): InstructionUiProjection {
    val relevant = instructions.relevantTo(currentStaffId, assignments)
    val details = relevant.associate { instruction ->
        val targetName = instruction.targetLabel(points, currentStaffId)
        instruction.instructionId to InstructionDetailUiModel(
            id = instruction.instructionId,
            title = instruction.title,
            body = instruction.body,
            target = ContactTargetUiModel(
                id = instruction.targetId ?: instruction.instructionId,
                type = if (instruction.targetId in points.map { it.pointId }) ContactTargetType.PLACE else ContactTargetType.USER,
                displayName = targetName,
            ),
            statusLabel = instruction.status.toLabel(),
        )
    }
    val summaries = details.values.map { detail ->
        InstructionSummaryUiModel(
            id = detail.id,
            title = detail.title,
            targetName = detail.target.displayName,
            statusLabel = detail.statusLabel,
            preview = detail.body,
        )
    }
    val featured = relevant
        .filter { it.status == dev.usbharu.tolo_staff.streaming.OperationInstructionStatus.ISSUED }
        .maxByOrNull { it.updatedAt }
        ?.instructionId
        ?.let(details::get)
        ?.let { detail -> summaries.firstOrNull { it.id == detail.id } }
    return InstructionUiProjection(
        state = InstructionsTabUiState(
            instructions = summaries,
            featuredInstruction = featured,
            otherInstructions = summaries.filterNot { it.id == featured?.id },
        ),
        details = details,
    )
}

internal data class ContactUiProjection(
    val state: ContactsTabUiState,
    val details: Map<String, ContactThreadDetailUiModel>,
)

internal fun buildContactUiProjection(
    currentStaffId: String,
    points: List<OperationPoint>,
    staff: List<OperationStaff>,
    threads: List<OperationThread>,
    messages: List<OperationMessage>,
): ContactUiProjection {
    val messagesByThread = messages.groupBy { it.threadId }
    val visibleThreads = threads.filter {
        currentStaffId in it.members && it.status != OperationThreadStatus.ARCHIVED
    }
    val details = visibleThreads.associate { thread ->
        val target = thread.toContactTarget(currentStaffId, points, staff)
        thread.threadId to ContactThreadDetailUiModel(
            id = thread.threadId,
            title = thread.deriveTitle(currentStaffId, staff),
            target = target,
            messages = messagesByThread[thread.threadId]
                .orEmpty()
                .sortedOperationMessages()
                .map { message ->
                    ThreadMessageUiModel(
                        id = message.messageId,
                        senderName = staff.firstOrNull { it.staffId == message.staffId }?.name ?: message.staffId,
                        body = message.toPreviewBody(),
                        timeLabel = message.updatedAt.ifBlank { null },
                        isCurrentUser = message.staffId == currentStaffId,
                    )
                },
            canReply = thread.status == OperationThreadStatus.OPEN,
        )
    }
    val summaries = details.values.map { detail ->
        ContactThreadSummaryUiModel(
            id = detail.id,
            title = detail.title,
            target = detail.target,
            lastMessagePreview = detail.messages.lastOrNull()?.body.orEmpty(),
        )
    }
    val targets = buildList {
        addAll(points.map { ContactTargetUiModel(it.pointId, ContactTargetType.PLACE, it.name) })
        addAll(staff.filterNot { it.staffId == currentStaffId }.map {
            ContactTargetUiModel(it.staffId, ContactTargetType.USER, it.name)
        })
    }
    return ContactUiProjection(
        state = ContactsTabUiState(threads = summaries, availableTargets = targets),
        details = details,
    )
}

private fun OperationThread.toContactTarget(
    currentStaffId: String,
    points: List<OperationPoint>,
    staff: List<OperationStaff>,
): ContactTargetUiModel = when (kind) {
    OperationThreadKind.LOCATION -> {
        val point = points.firstOrNull { it.pointId == pointId }
        ContactTargetUiModel(pointId ?: threadId, ContactTargetType.PLACE, point?.name ?: displayTitle)
    }
    else -> {
        val otherId = members.firstOrNull { it != currentStaffId }
        val name = staff.firstOrNull { it.staffId == otherId }?.name ?: displayTitle.ifBlank { otherId ?: threadId }
        ContactTargetUiModel(otherId ?: threadId, ContactTargetType.USER, name)
    }
}

internal fun List<OperationAssignment>.assignedPointIds(currentStaffId: String): Set<String> =
    filter {
        it.staffId == currentStaffId &&
            it.status in setOf(OperationAssignmentStatus.ACTIVE, OperationAssignmentStatus.SCHEDULED)
    }.mapTo(mutableSetOf()) { it.pointId }
