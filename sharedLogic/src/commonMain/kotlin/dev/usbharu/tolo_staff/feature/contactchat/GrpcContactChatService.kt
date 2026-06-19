package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GrpcContactChatService(
    private val dataSource: OperationsStreamDataSource,
    grpcClient: GrpcCommunicationClient,
) : ContactChatService {
    private val logger = AppLogger.withTag("GrpcContactChatService")
    private val writer = GrpcContactChatWriter(grpcClient = grpcClient)

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> {
        logger.info { "observeRooms started: currentStaffId=$currentStaffId" }
        dataSource.start()
        return combine(
            dataSource.observeThreads(),
            dataSource.observeMessages(currentStaffId),
            dataSource.observeStaff(),
        ) { threads, messages, staff ->
            val previewByThread = messages.toRoomPreviewByThread()
            threads
                .filter { currentStaffId in it.members }
                .sortedByLatestMessage(messages)
                .map { thread ->
                    ChatRoom(
                        id = thread.threadId,
                        title = thread.deriveTitle(currentStaffId, staff),
                        lastMessage = previewByThread[thread.threadId].orEmpty(),
                        unreadCount = 0,
                    )
                }
                .also { logger.debug { "Built chat rooms from stream: currentStaffId=$currentStaffId, roomCount=${it.size}" } }
        }
    }

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> {
        logger.info { "observeMessages started: roomId=$roomId, currentStaffId=$currentStaffId" }
        dataSource.start()
        return combine(
            dataSource.observeMessages(currentStaffId),
            dataSource.observeStaff(),
        ) { messages, staff ->
            messages
                .filter { it.threadId == roomId }
                .sortedOperationMessages()
                .map { message ->
                    message.toUiMessage(currentStaffId, staff)
                }
                .also { logger.debug { "Built chat messages from stream: roomId=$roomId, count=${it.size}" } }
        }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        logger.info { "Sending chat message via gRPC: roomId=$roomId, currentStaffId=$currentStaffId, textLength=${text.length}" }
        writer.sendSimpleMessage(roomId, currentStaffId, text)
        logger.info { "Sent chat message via gRPC: roomId=$roomId, currentStaffId=$currentStaffId" }
    }
}
