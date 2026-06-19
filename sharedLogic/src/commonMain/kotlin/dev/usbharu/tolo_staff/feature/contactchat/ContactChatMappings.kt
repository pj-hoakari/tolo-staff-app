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
    displayTitle
        .takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
        ?.let { return it }

    val namesById = staff.associate { it.staffId to it.name }
    val otherMembers = members
        .filterNot { it == currentStaffId }
        .distinct()
        .map { memberId -> namesById[memberId] ?: memberId }
    return otherMembers.joinToString(", ").ifBlank { threadId }
}

internal fun List<OperationMessage>.latestMessageByThread(): Map<String, OperationMessage?> = groupBy { it.threadId }
    .mapValues { (_, messages) -> messages.sortedOperationMessages().lastOrNull() }

internal fun List<OperationMessage>.toRoomPreviewByThread(): Map<String, String> = latestMessageByThread()
    .mapValues { (_, message) -> message?.toPreviewBody().orEmpty() }

internal fun List<OperationThread>.sortedByLatestMessage(messages: List<OperationMessage>): List<OperationThread> {
    val latestMessagesByThread = messages.latestMessageByThread()
    return sortedWith(
        compareByDescending<OperationThread> { thread ->
            latestMessagesByThread[thread.threadId]?.updatedAt?.takeUnless {
                it.equals("null", ignoreCase = true) || it.isBlank()
            }
        }.thenBy { it.threadId }
    )
}

internal fun OperationMessage.toUiMessage(
    currentStaffId: String,
    staff: List<OperationStaff>,
): ChatMessage {
    logger.trace {
        "toUiMessage started: messageId=$messageId, threadId=$threadId, currentStaffId=$currentStaffId, rawSenderStaffId=$staffId, rawSenderName=$senderName"
    }
    val effectiveSenderStaffId = normalizedSenderStaffId()
    val effectiveSenderName = senderName
        ?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
    val resolvedSenderName = staff.firstOrNull { it.staffId == effectiveSenderStaffId }?.name
        ?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
        ?: effectiveSenderName
        ?: effectiveSenderStaffId
        ?: "不明"
    val currentStaffName = staff.firstOrNull { it.staffId == currentStaffId }?.name
        ?.takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
    val isFromCurrentUser = effectiveSenderStaffId == currentStaffId ||
        (
            effectiveSenderStaffId == null &&
                effectiveSenderName != null &&
                currentStaffName != null &&
                effectiveSenderName == currentStaffName
            )
    if (resolvedSenderName == "不明") {
        logger.warn {
            "Unable to resolve sender for chat message: messageId=$messageId, threadId=$threadId, currentStaffId=$currentStaffId, rawSenderStaffId=$staffId, normalizedSenderStaffId=$effectiveSenderStaffId, rawSenderName=$senderName, currentStaffName=$currentStaffName, messageType=$messageType"
        }
    }
    logger.debug {
        "Mapped chat message: messageId=$messageId, threadId=$threadId, senderStaffId=$staffId, effectiveSenderStaffId=$effectiveSenderStaffId, currentStaffId=$currentStaffId, senderName=$resolvedSenderName, isFromCurrentUser=$isFromCurrentUser"
    }

    return ChatMessage(
        id = messageId,
        roomId = threadId,
        senderName = resolvedSenderName,
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
