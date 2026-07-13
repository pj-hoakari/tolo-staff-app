package dev.usbharu.tolo_staff.streaming

import kotlinx.serialization.Serializable

interface OperationsStreamEntity {
    val updatedAt: String
    val reason: String
    val entityId: String
}

@Serializable
data class OperationPoint(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val pointId: String,
    val name: String,
) : OperationsStreamEntity

@Serializable
data class OperationStaff(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val staffId: String,
    val name: String,
) : OperationsStreamEntity

@Serializable
enum class OperationAssignmentStatus {
    SCHEDULED,
    ACTIVE,
    ENDED,
    CANCELLED,
}

@Serializable
data class OperationAssignment(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val assignId: String,
    val pointId: String,
    val staffId: String,
    val status: OperationAssignmentStatus,
    val version: Long = 0,
) : OperationsStreamEntity

@Serializable
enum class OperationInstructionStatus {
    ISSUED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
}

@Serializable
enum class OperationInstructionTargetType {
    ALL_EVENT_STAFF,
    STAFF,
    POINT,
    UNKNOWN,
}

@Serializable
data class OperationInstruction(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val instructionId: String,
    val targetType: OperationInstructionTargetType,
    val targetId: String? = null,
    val body: String,
    val status: OperationInstructionStatus,
    val version: Long = 0,
) : OperationsStreamEntity {
    val title: String
        get() = body.lineSequence().firstOrNull().orEmpty().take(MAX_TITLE_LENGTH)

    private companion object {
        const val MAX_TITLE_LENGTH = 40
    }
}

@Serializable
enum class OperationThreadKind {
    GENERAL,
    LOCATION,
    DIRECT,
    UNKNOWN,
}

@Serializable
enum class OperationThreadStatus {
    OPEN,
    CLOSED,
    ARCHIVED,
}

@Serializable
data class OperationThread(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val threadId: String,
    val members: List<String> = emptyList(),
    val displayTitle: String = "",
    val kind: OperationThreadKind = OperationThreadKind.UNKNOWN,
    val pointId: String? = null,
    val status: OperationThreadStatus = OperationThreadStatus.OPEN,
    val version: Long = 0,
) : OperationsStreamEntity

@Serializable
data class OperationMessage(
    override val updatedAt: String = "",
    override val reason: String = "",
    override val entityId: String = "",
    val messageId: String,
    val threadId: String,
    val staffId: String,
    val text: String,
    val senderName: String? = null,
) : OperationsStreamEntity
