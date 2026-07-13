package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo.communication.v1.AssignmentService
import dev.usbharu.tolo.communication.v1.InstructionService
import dev.usbharu.tolo.communication.v1.MessageService
import dev.usbharu.tolo.communication.v1.ReferenceService
import dev.usbharu.tolo.communication.v1.ReportService
import dev.usbharu.tolo.communication.v1.ThreadService
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider
import dev.usbharu.tolo_staff.runtime.NoOpGrpcMetadataProvider
import kotlinx.rpc.grpc.GrpcMetadata
import kotlinx.rpc.grpc.append
import kotlinx.rpc.grpc.client.GrpcCallCredentials
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.grpc.client.plus
import kotlinx.rpc.withService

class GrpcCommunicationClient(
    host: String,
    port: Int,
    val metadataProvider: GrpcMetadataProvider = NoOpGrpcMetadataProvider,
) {
    private val logger = AppLogger.withTag("GrpcCommunicationClient")
    private val client = GrpcClient(host, port) {
        credentials = plaintext() + MetadataCallCredentials(metadataProvider)
    }

    init {
        logger.info { "Configured gRPC client: host=$host, port=$port" }
    }

    val referenceService: ReferenceService by lazy { client.withService<ReferenceService>() }
    val assignmentService: AssignmentService by lazy { client.withService<AssignmentService>() }
    val instructionService: InstructionService by lazy { client.withService<InstructionService>() }
    val threadService: ThreadService by lazy { client.withService<ThreadService>() }
    val messageService: MessageService by lazy { client.withService<MessageService>() }
    val reportService: ReportService by lazy { client.withService<ReportService>() }
}

private class MetadataCallCredentials(
    private val provider: GrpcMetadataProvider,
) : GrpcCallCredentials {
    override suspend fun GrpcCallCredentials.Context.getRequestMetadata(): GrpcMetadata {
        val values = provider.metadata()
        return GrpcMetadata {
            values.forEach { (name, value) -> append(name, value) }
        }
    }

    override val requiresTransportSecurity: Boolean = false
}
