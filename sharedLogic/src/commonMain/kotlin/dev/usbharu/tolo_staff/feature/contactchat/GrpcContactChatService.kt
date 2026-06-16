package dev.usbharu.tolo_staff.feature.contactchat

import com.google.protobuf.kotlin.Empty
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateMessageRequest
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.SimpleMessagePayload
import dev.usbharu.tolo.communication.grpc.Thread
import dev.usbharu.tolo.communication.grpc.ThreadIdRequest
import dev.usbharu.tolo.communication.grpc.ThreadRpc
import dev.usbharu.tolo.communication.grpc.invoke
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService
import kotlin.random.Random

class GrpcContactChatService(
    host: String = contactChatServerHost,
    private val port: Int = 8080,
) : ContactChatService {
    private val grpcClient = GrpcClient(host, port) {
        credentials = plaintext()
    }

    private val threadService = grpcClient.withService<ThreadRpc>()
    private val messageService = grpcClient.withService<MessageRpc>()

    override suspend fun listRooms(currentStaffId: String): List<ChatRoom> {
        val threads = threadService.ListThreads(Empty {})
            .threads
            .filter { currentStaffId in it.memberStaffIds }

        return threads.map { thread ->
            val messages = listMessages(thread.threadId, currentStaffId)
            ChatRoom(
                id = thread.threadId,
                title = thread.deriveTitle(currentStaffId),
                lastMessage = messages.lastOrNull()?.body,
                unreadCount = 0,
            )
        }
    }

    override suspend fun listMessages(roomId: String, currentStaffId: String): List<ChatMessage> {
        return threadService.ListThreadMessages(
            ThreadIdRequest { threadId = roomId }
        ).messages.map { message ->
            message.toUiMessage(currentStaffId)
        }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        val messageId = buildMessageId(currentStaffId)
        messageService.CreateMessage(
            CreateMessageRequest {
                message = Message {
                    this.messageId = messageId
                    threadId = roomId
                    staffId = currentStaffId
                    payload = Message.Payload.Simple(
                        SimpleMessagePayload {
                            this.text = text
                            replyTo = messageId
                        }
                    )
                }
            }
        )
    }

    private fun buildMessageId(currentStaffId: String): String =
        "client-$currentStaffId-${Random.nextLong().toString().replace('-', '0')}"
}

private fun Thread.deriveTitle(currentStaffId: String): String {
    val otherMembers = memberStaffIds
        .filterNot { it == currentStaffId }
        .distinct()
    return otherMembers.joinToString(", ").ifBlank { threadId }
}

private fun Message.toUiMessage(currentStaffId: String): ChatMessage {
    val payload = payload
    val body = when (payload) {
        is Message.Payload.Simple -> payload.value.text
        is Message.Payload.Assign -> "配属が更新されました"
        is Message.Payload.Unassign -> "配属解除が共有されました"
        is Message.Payload.Instruction -> "指示が共有されました: ${payload.value.instructionId}"
        is Message.Payload.Report -> "報告が共有されました: ${payload.value.reportId}"
        null -> "不明なメッセージ"
    }

    return ChatMessage(
        id = messageId,
        roomId = threadId,
        senderName = staffId,
        body = body,
        timeLabel = null,
        isFromCurrentUser = staffId == currentStaffId,
        isSystemEvent = payload !is Message.Payload.Simple,
    )
}
