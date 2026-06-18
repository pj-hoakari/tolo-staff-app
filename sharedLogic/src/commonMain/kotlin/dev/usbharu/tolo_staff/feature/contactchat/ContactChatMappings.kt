package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages

internal fun OperationThread.deriveTitle(
    currentStaffId: String,
    staff: List<OperationStaff>,
): String {
    val namesById = staff.associate { it.staffId to it.name }
    val otherMembers = members
        .filterNot { it == currentStaffId }
        .distinct()
        .map { memberId -> namesById[memberId] ?: memberId }
    return otherMembers.joinToString(", ").ifBlank { threadId }
}

internal fun List<OperationMessage>.toRoomPreviewByThread(): Map<String, String> = groupBy { it.threadId }
    .mapValues { (_, messages) -> messages.sortedOperationMessages().lastOrNull()?.toPreviewBody().orEmpty() }

internal fun OperationMessage.toUiMessage(
    currentStaffId: String,
    staff: List<OperationStaff>,
): ChatMessage {
    val senderName = staff.firstOrNull { it.staffId == staffId }?.name ?: staffId

    return ChatMessage(
        id = messageId,
        roomId = threadId,
        senderName = senderName,
        body = toPreviewBody(),
        timeLabel = updatedAt,
        isFromCurrentUser = staffId == currentStaffId,
        isSystemEvent = messageType != OperationMessageType.SIMPLE,
    )
}

internal fun OperationMessage.toPreviewBody(): String = when (messageType) {
    OperationMessageType.SIMPLE -> text ?: "メッセージ"
    OperationMessageType.ASSIGN -> "配属が更新されました"
    OperationMessageType.UNASSIGN -> "配属解除が共有されました"
    OperationMessageType.INSTRUCTION -> "指示が共有されました: ${instructionId.orEmpty()}"
    OperationMessageType.REPORT -> "報告が共有されました: ${reportId.orEmpty()}"
}
