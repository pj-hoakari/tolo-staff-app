package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages

private val logger = AppLogger.withTag("ContactChatMappings")

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
    val effectiveSenderStaffId = normalizedSenderStaffId()
    val senderName = staff.firstOrNull { it.staffId == effectiveSenderStaffId }?.name
        ?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
        ?: effectiveSenderStaffId
        ?: "不明"
    val isFromCurrentUser = effectiveSenderStaffId == currentStaffId
    logger.debug {
        "Mapped chat message: messageId=$messageId, threadId=$threadId, senderStaffId=$staffId, effectiveSenderStaffId=$effectiveSenderStaffId, currentStaffId=$currentStaffId, senderName=$senderName, isFromCurrentUser=$isFromCurrentUser"
    }

    return ChatMessage(
        id = messageId,
        roomId = threadId,
        senderName = senderName,
        body = toPreviewBody(),
        timeLabel = updatedAt.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() },
        isFromCurrentUser = isFromCurrentUser,
        isSystemEvent = messageType != OperationMessageType.SIMPLE,
    )
}

internal fun OperationMessage.normalizedSenderStaffId(): String? =
    staffId
        .takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
        ?: messageId.clientSenderStaffId()

private fun String.clientSenderStaffId(): String? {
    if (!startsWith("client-")) {
        return null
    }
    return removePrefix("client-").substringBeforeLast("-").takeIf { it.isNotBlank() }
}

internal fun OperationMessage.toPreviewBody(): String = when (messageType) {
    OperationMessageType.SIMPLE -> text?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() } ?: "メッセージ"
    OperationMessageType.ASSIGN -> "配属が更新されました"
    OperationMessageType.UNASSIGN -> "配属解除が共有されました"
    OperationMessageType.INSTRUCTION -> "指示が共有されました: ${instructionId.orEmpty()}"
    OperationMessageType.REPORT -> "報告が共有されました: ${reportId.orEmpty()}"
}
