package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo.communication.v1.CreateThreadRequest
import dev.usbharu.tolo.communication.v1.MessageAudience
import dev.usbharu.tolo.communication.v1.SendMessageRequest
import dev.usbharu.tolo.communication.v1.ThreadKind
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.DefaultRequestIdGenerator
import dev.usbharu.tolo_staff.runtime.RequestIdGenerator
import dev.usbharu.tolo_staff.streaming.GrpcCommunicationClient
import dev.usbharu.tolo_staff.streaming.toProtoScope
import dev.usbharu.tolo_staff.streaming.toProtoPointId
import dev.usbharu.tolo_staff.streaming.toProtoStaffId
import dev.usbharu.tolo_staff.streaming.toProtoThreadId

internal class GrpcContactChatWriter(
    private val grpcClient: GrpcCommunicationClient,
    private val session: AppSessionContext,
    private val requestIdGenerator: RequestIdGenerator = DefaultRequestIdGenerator,
) {
    suspend fun sendMessage(threadId: String, text: String) {
        grpcClient.messageService.SendMessage(
            sendMessageRequest(session, requestIdGenerator.nextId(), threadId, text)
        )
    }

    suspend fun createThread(target: ChatThreadTarget): ChatRoom {
        val thread = grpcClient.threadService.CreateThread(
            createThreadRequest(session, requestIdGenerator.nextId(), target)
        )
        return ChatRoom(id = thread.id.value, title = thread.title)
    }
}

internal fun sendMessageRequest(
    session: AppSessionContext,
    requestId: String,
    threadId: String,
    body: String,
): SendMessageRequest = SendMessageRequest {
    scope = session.toProtoScope()
    this.requestId = requestId
    this.threadId = threadId.toProtoThreadId()
    audience = MessageAudience.MESSAGE_AUDIENCE_STAFF
    this.body = body
}

internal fun createThreadRequest(
    session: AppSessionContext,
    requestId: String,
    target: ChatThreadTarget,
): CreateThreadRequest = CreateThreadRequest {
    scope = session.toProtoScope()
    this.requestId = requestId
    title = target.title
    kind = when (target.kind) {
        ChatThreadTargetKind.DIRECT -> ThreadKind.THREAD_KIND_DIRECT
        ChatThreadTargetKind.LOCATION -> ThreadKind.THREAD_KIND_LOCATION
    }
    participantStaffIds += session.staffId.toProtoStaffId()
    if (target.kind == ChatThreadTargetKind.DIRECT) {
        participantStaffIds += target.id.toProtoStaffId()
    } else {
        pointId = target.id.toProtoPointId()
    }
}
