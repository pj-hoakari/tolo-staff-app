package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GrpcContactChatService(
    private val dataSource: OperationsStreamDataSource,
    grpcClient: GrpcCommunicationClient,
    session: AppSessionContext,
) : ContactChatService {
    private val logger = AppLogger.withTag("GrpcContactChatService")
    private val writer = GrpcContactChatWriter(grpcClient = grpcClient, session = session)

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
            dataSource.observeThreadMessages(roomId, currentStaffId),
            dataSource.observeStaff(),
        ) { messages, staff ->
            messages
                .filter { it.threadId == roomId }
                .sortedOperationMessages()
                .map { message -> message.toUiMessage(currentStaffId, staff) }
                .also { logger.debug { "Built chat messages from stream: roomId=$roomId, count=${it.size}" } }
        }
    }

    override suspend fun sendMessage(threadId: String, body: String) {
        logger.info { "Sending chat message via gRPC: threadId=$threadId, textLength=${body.length}" }
        writer.sendMessage(threadId, body)
        logger.info { "Sent chat message via gRPC: threadId=$threadId" }
    }

    override suspend fun createThread(target: ChatThreadTarget): ChatRoom = writer.createThread(target)
}
