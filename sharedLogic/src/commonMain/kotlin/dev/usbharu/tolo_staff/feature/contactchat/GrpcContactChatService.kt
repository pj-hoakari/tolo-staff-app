package dev.usbharu.tolo_staff.feature.contactchat

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateMessageRequest
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.SimpleMessagePayload
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.OperationMessageType
import dev.usbharu.tolo_staff.streaming.OperationStaff
import dev.usbharu.tolo_staff.streaming.OperationThread
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.random.Random

class GrpcContactChatService(
    host: String = contactChatServerHost,
    private val port: Int = 8080,
    private val dataSource: OperationsStreamDataSource,
) : ContactChatService {
    private val grpcHost = host

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> {
        dataSource.start()
        return combine(
            dataSource.observeThreads(),
            dataSource.observeMessages(),
            dataSource.observeStaff(),
        ) { threads, messages, staff ->
            threads
                .filter { currentStaffId in it.members }
                .sortedBy { it.threadId }
                .map { thread ->
                    val threadMessages = messages
                        .filter { it.threadId == thread.threadId }
                        .sortedWith(compareBy<OperationMessage> { it.updatedAt }.thenBy { it.messageId })
                    ChatRoom(
                        id = thread.threadId,
                        title = thread.deriveTitle(currentStaffId, staff),
                        lastMessage = threadMessages.lastOrNull()?.toPreviewBody(),
                        unreadCount = 0,
                    )
                }
        }
    }

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> {
        dataSource.start()
        return combine(
            dataSource.observeMessages(),
            dataSource.observeStaff(),
        ) { messages, staff ->
            messages
                .filter { it.threadId == roomId }
                .sortedWith(compareBy<OperationMessage> { it.updatedAt }.thenBy { it.messageId })
                .map { message ->
                    message.toUiMessage(currentStaffId, staff)
                }
        }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        createMessageService().CreateMessage(
            CreateMessageRequest {
                message = Message {
                    this.messageId = buildMessageId(currentStaffId)
                    threadId = roomId
                    staffId = currentStaffId
                    payload = Message.Payload.Simple(
                        SimpleMessagePayload {
                            this.text = text
                            replyTo = ""
                        }
                    )
                }
            }
        )
    }

    private fun buildMessageId(currentStaffId: String): String =
        "client-$currentStaffId-${Random.nextLong().toString().replace('-', '0')}"

    private fun createMessageService(): MessageRpc {
        val grpcClient = GrpcClient(grpcHost, port) {
            credentials = plaintext()
        }
        return grpcClient.withService<MessageRpc>()
    }
}

private fun OperationThread.deriveTitle(
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

private fun OperationMessage.toUiMessage(
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

private fun OperationMessage.toPreviewBody(): String = when (messageType) {
    OperationMessageType.SIMPLE -> text ?: "メッセージ"
    OperationMessageType.ASSIGN -> "配属が更新されました"
    OperationMessageType.UNASSIGN -> "配属解除が共有されました"
    OperationMessageType.INSTRUCTION -> "指示が共有されました: ${instructionId.orEmpty()}"
    OperationMessageType.REPORT -> "報告が共有されました: ${reportId.orEmpty()}"
}
