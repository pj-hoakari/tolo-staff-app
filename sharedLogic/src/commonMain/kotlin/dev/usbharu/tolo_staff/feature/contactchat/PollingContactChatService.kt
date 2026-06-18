package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.streaming.OperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.OperationsPollingRemoteDataSource
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.OperationMessage
import dev.usbharu.tolo_staff.streaming.SharedPollingFlowFactory
import dev.usbharu.tolo_staff.streaming.defaultOperationsPollingConfig
import dev.usbharu.tolo_staff.streaming.sortedOperationMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PollingContactChatService(
    private val remoteDataSource: OperationsPollingRemoteDataSource,
    private val config: OperationsPollingConfig = defaultOperationsPollingConfig(),
    grpcClient: GrpcCommunicationClient,
) : ContactChatService {
    private val writer = GrpcContactChatWriter(grpcClient = grpcClient)
    private val pollingFactory = SharedPollingFlowFactory(config.intervalMillis)
    private val staffFlow by lazy { pollingFactory.create { remoteDataSource.listStaff() } }
    private val threadsFlow by lazy { pollingFactory.create { remoteDataSource.listThreads() } }
    private val messagesFlow by lazy {
        pollingFactory.create { remoteDataSource.listMessages().sortedOperationMessages() }
    }
    private val roomMessagesFlows = mutableMapOf<String, Flow<List<OperationMessage>>>()

    override fun observeRooms(currentStaffId: String): Flow<List<ChatRoom>> = combine(
        threadsFlow,
        messagesFlow,
        staffFlow,
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

    override fun observeMessages(roomId: String, currentStaffId: String): Flow<List<ChatMessage>> = combine(
        roomMessagesFlow(roomId),
        staffFlow,
    ) { messages, staff ->
        messages
            .sortedOperationMessages()
            .map { it.toUiMessage(currentStaffId, staff) }
    }.flowOn(Dispatchers.Default)

    override suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        withContext(Dispatchers.Default) {
            writer.sendSimpleMessage(roomId, currentStaffId, text)
        }
    }

    private fun roomMessagesFlow(roomId: String): Flow<List<OperationMessage>> =
        roomMessagesFlows.getOrPut(roomId) {
            pollingFactory.create { remoteDataSource.listThreadMessages(roomId).sortedOperationMessages() }
        }
}
