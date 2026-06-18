package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.OperationsStreamDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GrpcContactChatService(
    private val dataSource: OperationsStreamDataSource,
    grpcClient: GrpcCommunicationClient,
) : ContactChatService {
    private val writer = GrpcContactChatWriter(grpcClient = grpcClient)

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> {
        dataSource.start()
        return combine(
            dataSource.observeThreads(),
            dataSource.observeMessages(),
            dataSource.observeStaff(),
        ) { threads, messages, staff ->
            val previewByThread = messages.toRoomPreviewByThread()
            threads
                .filter { currentStaffId in it.members }
                .sortedBy { it.threadId }
                .map { thread ->
                    ChatRoom(
                        id = thread.threadId,
                        title = thread.deriveTitle(currentStaffId, staff),
                        lastMessage = previewByThread[thread.threadId].orEmpty(),
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
                .sortedOperationMessages()
                .map { message ->
                    message.toUiMessage(currentStaffId, staff)
                }
        }
    }

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        writer.sendSimpleMessage(roomId, currentStaffId, text)
    }
}
