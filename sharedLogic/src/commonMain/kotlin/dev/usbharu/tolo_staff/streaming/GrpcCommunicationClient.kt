package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo.communication.grpc.AssignmentRpc
import dev.usbharu.tolo.communication.grpc.InstructionRpc
import dev.usbharu.tolo.communication.grpc.MessageRpc
import dev.usbharu.tolo.communication.grpc.PointRpc
import dev.usbharu.tolo.communication.grpc.ReportRpc
import dev.usbharu.tolo.communication.grpc.StaffRpc
import dev.usbharu.tolo.communication.grpc.ThreadRpc
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService

class GrpcCommunicationClient(
    host: String,
    port: Int,
) {
    private val client = GrpcClient(host, port) {
        credentials = plaintext()
    }

    val pointService: PointRpc by lazy { client.withService<PointRpc>() }
    val staffService: StaffRpc by lazy { client.withService<StaffRpc>() }
    val assignmentService: AssignmentRpc by lazy { client.withService<AssignmentRpc>() }
    val instructionService: InstructionRpc by lazy { client.withService<InstructionRpc>() }
    val threadService: ThreadRpc by lazy { client.withService<ThreadRpc>() }
    val messageService: MessageRpc by lazy { client.withService<MessageRpc>() }
    val reportService: ReportRpc by lazy { client.withService<ReportRpc>() }
}
