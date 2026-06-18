package dev.usbharu.tolo_staff.feature.contactchat

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateMessageRequest
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.SimpleMessagePayload
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import kotlin.random.Random

class GrpcContactChatWriter(
    private val grpcClient: GrpcCommunicationClient,
) {
    suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        grpcClient.messageService.CreateMessage(
            CreateMessageRequest {
                message = Message {
                    this.messageId = buildMessageId(currentStaffId)
                    threadId = roomId
                    staffId = currentStaffId
                    payload = Message.Payload.Simple(
                        SimpleMessagePayload {
                            this.text = text
                        }
                    )
                }
            }
        )
    }

    private fun buildMessageId(currentStaffId: String): String =
        "client-$currentStaffId-${Random.nextLong().toString().replace('-', '0')}"
}
