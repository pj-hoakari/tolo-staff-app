package dev.usbharu.tolo_staff.streaming

import kotlinx.serialization.Serializable

interface OperationsStreamEntity {
    val updatedAt: String
    val reason: String
    val entityId: String
}

@Serializable
data class OperationPoint(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val pointId: String,
    val name: String,
    val description: String,
) : OperationsStreamEntity

@Serializable
data class OperationStaff(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val staffId: String,
    val name: String,
    val roles: List<String> = emptyList(),
) : OperationsStreamEntity

@Serializable
enum class OperationAssignmentStatus {
    ACTIVE,
    EN_ROUTE,
    PENDING,
}

@Serializable
data class OperationAssignment(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val assignId: String,
    val pointId: String,
    val staffId: String,
    val status: OperationAssignmentStatus,
) : OperationsStreamEntity

@Serializable
enum class OperationInstructionStatus {
    ACTIVE,
}

@Serializable
data class OperationInstruction(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val instructionId: String,
    val pointIds: List<String> = emptyList(),
    val staffIds: List<String> = emptyList(),
    val title: String,
    val description: String,
    val status: OperationInstructionStatus,
) : OperationsStreamEntity

@Serializable
data class OperationThread(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val threadId: String,
    val members: List<String> = emptyList(),
) : OperationsStreamEntity

@Serializable
enum class OperationMessageType {
    ASSIGN,
    UNASSIGN,
    INSTRUCTION,
    REPORT,
    SIMPLE,
}

@Serializable
data class OperationMessage(
    override val updatedAt: String,
    override val reason: String,
    override val entityId: String,
    val messageId: String,
    val threadId: String,
    val staffId: String,
    val messageType: OperationMessageType,
    val instructionId: String? = null,
    val reportId: String? = null,
    val text: String? = null,
    val replyTo: String? = null,
) : OperationsStreamEntity
