package dev.usbharu.tolo_staff.feature.contactchat

import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.CreateMessageRequest
import dev.usbharu.tolo.communication.grpc.Message
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.SimpleMessagePayload
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import kotlin.random.Random

class GrpcContactChatWriter(
    private val grpcClient: GrpcCommunicationClient,
) {
    private val logger = AppLogger.withTag("GrpcContactChatWriter")

    suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        logger.trace {
            "sendSimpleMessage started: roomId=$roomId, currentStaffId=$currentStaffId, textLength=${text.length}"
        }
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
        logger.trace { "sendSimpleMessage completed: roomId=$roomId, currentStaffId=$currentStaffId" }
    }

    private fun buildMessageId(currentStaffId: String): String =
        "client-$currentStaffId-${Random.nextLong().toString().replace('-', '0')}"
}
