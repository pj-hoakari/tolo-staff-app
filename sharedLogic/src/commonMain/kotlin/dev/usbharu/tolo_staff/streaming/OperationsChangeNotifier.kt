package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class OperationEntityType {
    POINT,
    STAFF,
    ASSIGNMENT,
    THREAD,
    MESSAGE,
    INSTRUCTION,
    REPORT,
    UNKNOWN,
    ;

    companion object
}

enum class OperationChangeType {
    CREATED,
    UPDATED,
    DELETED,
    UNKNOWN,
    ;

    companion object
}

data class OperationsChangeHead(
    val version: Long = 0L,
    val updatedAt: String = "",
    val entityType: OperationEntityType = OperationEntityType.UNKNOWN,
    val entityId: String = "",
    val reason: String = "",
    val threadId: String? = null,
    val operation: OperationChangeType = OperationChangeType.UNKNOWN,
)

interface OperationsChangeNotifier {
    fun observeGlobal(): Flow<OperationsChangeHead> = emptyFlow()

    fun observeStaff(staffId: String): Flow<OperationsChangeHead> = emptyFlow()
}

class NoOpOperationsChangeNotifier : OperationsChangeNotifier
