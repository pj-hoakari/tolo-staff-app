package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService
import tolo.operation.v1.DeliveryCoordinationService
import tolo.operation.v1.DigestService
import tolo.operation.v1.OperationControlService
import tolo.operation.v1.StaffCommunicationService
import tolo.realtime.v1.RealtimeFetchService

class GrpcCommunicationClient(
    host: String,
    port: Int,
) {
    private val logger = AppLogger.withTag("GrpcCommunicationClient")
    private val client = GrpcClient(host, port) {
        credentials = plaintext()
    }

    init {
        logger.info { "Configured gRPC client: host=$host, port=$port" }
    }

    val staffCommunicationService: StaffCommunicationService by lazy {
        client.withService<StaffCommunicationService>()
    }
    val operationControlService: OperationControlService by lazy {
        client.withService<OperationControlService>()
    }
    val deliveryCoordinationService: DeliveryCoordinationService by lazy {
        client.withService<DeliveryCoordinationService>()
    }
    val digestService: DigestService by lazy {
        client.withService<DigestService>()
    }
    val realtimeFetchService: RealtimeFetchService by lazy {
        client.withService<RealtimeFetchService>()
    }
}
