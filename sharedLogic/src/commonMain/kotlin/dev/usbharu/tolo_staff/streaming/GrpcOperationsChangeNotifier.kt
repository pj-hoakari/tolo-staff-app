package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Empty
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.grpc.ChangeEntityType
import dev.usbharu.tolo.communication.grpc.ChangeHead
import dev.usbharu.tolo.communication.grpc.ChangeOperation
import dev.usbharu.tolo.communication.grpc.StaffIdRequest
import dev.usbharu.tolo.communication.grpc.invoke
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class GrpcOperationsChangeNotifier(
    private val fetchGlobalChangeHead: suspend () -> OperationsChangeHead,
    private val fetchStaffChangeHead: suspend (String) -> OperationsChangeHead,
    private val config: OperationsPollingConfig = defaultOperationsPollingConfig(),
) : OperationsChangeNotifier {
    private val logger = AppLogger.withTag("GrpcOperationsChangeNotifier")

    constructor(
        grpcClient: GrpcCommunicationClient,
        config: OperationsPollingConfig = defaultOperationsPollingConfig(),
    ) : this(
        fetchGlobalChangeHead = {
            grpcClient.changeHeadService.GetGlobalChangeHead(Empty {}).toOperationsChangeHead()
        },
        fetchStaffChangeHead = { staffId ->
            grpcClient.changeHeadService.GetStaffChangeHead(
                StaffIdRequest {
                    this.staffId = staffId
                }
            ).toOperationsChangeHead()
        },
        config = config,
    )

    override fun observeGlobal(): Flow<OperationsChangeHead> = pollingFlow(
        label = "global",
        fetch = fetchGlobalChangeHead,
    )

    override fun observeStaff(staffId: String): Flow<OperationsChangeHead> {
        if (staffId.isBlank() || staffId == UNKNOWN_STAFF_ID) {
            return emptyFlow()
        }
        return pollingFlow(
            label = "staff:$staffId",
            fetch = { fetchStaffChangeHead(staffId) },
        )
    }

    private fun pollingFlow(
        label: String,
        fetch: suspend () -> OperationsChangeHead,
    ): Flow<OperationsChangeHead> = flow {
        var lastVersion = INITIAL_VERSION
        while (currentCoroutineContext().isActive) {
            val head = try {
                withContext(Dispatchers.Default) { fetch() }
            } catch (throwable: Throwable) {
                logger.warn(throwable) { "Polling change head failed: label=$label" }
                throw throwable
            }
            if (head.version > lastVersion) {
                lastVersion = head.version
                emit(head)
            }
            delay(config.intervalMillis)
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
        const val INITIAL_VERSION = -1L
    }
}

internal fun ChangeHead.toOperationsChangeHead(): OperationsChangeHead = OperationsChangeHead(
    version = version,
    updatedAt = updatedAt.toIsoString(),
    entityType = entityType.toOperationEntityType(),
    entityId = entityId,
    reason = reason,
    threadId = threadId.ifBlank { null },
    operation = operation.toOperationChangeType(),
)

private fun ChangeEntityType.toOperationEntityType(): OperationEntityType =
    when (this) {
        ChangeEntityType.CHANGE_ENTITY_TYPE_POINT -> OperationEntityType.POINT
        ChangeEntityType.CHANGE_ENTITY_TYPE_STAFF -> OperationEntityType.STAFF
        ChangeEntityType.CHANGE_ENTITY_TYPE_ASSIGNMENT -> OperationEntityType.ASSIGNMENT
        ChangeEntityType.CHANGE_ENTITY_TYPE_THREAD -> OperationEntityType.THREAD
        ChangeEntityType.CHANGE_ENTITY_TYPE_MESSAGE -> OperationEntityType.MESSAGE
        ChangeEntityType.CHANGE_ENTITY_TYPE_INSTRUCTION -> OperationEntityType.INSTRUCTION
        ChangeEntityType.CHANGE_ENTITY_TYPE_REPORT -> OperationEntityType.REPORT
        is ChangeEntityType.UNRECOGNIZED -> OperationEntityType.UNKNOWN
        ChangeEntityType.CHANGE_ENTITY_TYPE_UNSPECIFIED -> OperationEntityType.UNKNOWN
    }

private fun ChangeOperation.toOperationChangeType(): OperationChangeType =
    when (this) {
        ChangeOperation.CHANGE_OPERATION_CREATED -> OperationChangeType.CREATED
        ChangeOperation.CHANGE_OPERATION_UPDATED -> OperationChangeType.UPDATED
        ChangeOperation.CHANGE_OPERATION_DELETED -> OperationChangeType.DELETED
        is ChangeOperation.UNRECOGNIZED -> OperationChangeType.UNKNOWN
        ChangeOperation.CHANGE_OPERATION_UNSPECIFIED -> OperationChangeType.UNKNOWN
    }
