package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages

internal fun OperationThread.deriveTitle(
    currentStaffId: String,
    staff: List<OperationStaff>,
): String {
    displayTitle.takeIf(String::isNotBlank)?.let { return it }
    val namesById = staff.associate { it.staffId to it.name }
    return members
        .filterNot { it == currentStaffId }
        .distinct()
        .joinToString(", ") { namesById[it] ?: it }
        .ifBlank { threadId }
}

internal fun List<OperationMessage>.latestMessageByThread(): Map<String, OperationMessage?> =
    groupBy { it.threadId }.mapValues { (_, messages) -> messages.sortedOperationMessages().lastOrNull() }

internal fun List<OperationMessage>.toRoomPreviewByThread(): Map<String, String> =
    latestMessageByThread().mapValues { (_, message) -> message?.toPreviewBody().orEmpty() }

internal fun List<OperationThread>.sortedByLatestMessage(messages: List<OperationMessage>): List<OperationThread> {
    val latestMessages = messages.latestMessageByThread()
    return sortedWith(
        compareByDescending<OperationThread> { latestMessages[it.threadId]?.updatedAt?.ifBlank { null } }
            .thenBy { it.threadId }
    )
}

internal fun OperationMessage.toUiMessage(
    currentStaffId: String,
    staff: List<OperationStaff>,
): ChatMessage {
    val sender = staff.firstOrNull { it.staffId == staffId }?.name
        ?: senderName?.takeIf(String::isNotBlank)
        ?: staffId.ifBlank { "不明" }
    return ChatMessage(
        id = messageId,
        roomId = threadId,
        senderName = sender,
        body = toPreviewBody(),
        timeLabel = updatedAt.ifBlank { null },
        isFromCurrentUser = staffId == currentStaffId,
        isSystemEvent = false,
    )
}

internal fun OperationMessage.normalizedSenderStaffId(): String? = staffId.ifBlank { null }

internal fun OperationMessage.toPreviewBody(): String = text.ifBlank { "メッセージ" }
