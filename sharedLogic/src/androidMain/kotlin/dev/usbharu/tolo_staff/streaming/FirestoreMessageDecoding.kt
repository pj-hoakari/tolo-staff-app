package dev.usbharu.tolo_staff.streaming

internal data class OperationMessageDecodedFields(
    val updatedAt: String,
    val reason: String,
    val entityId: String,
    val messageId: String,
    val threadId: String,
    val staffId: String,
    val messageType: OperationMessageType,
    val instructionId: String? = null,
    val reportId: String? = null,
    val text: String? = null,
    val replyTo: String? = null,
    val senderName: String? = null,
) {
    fun toOperationMessage(): OperationMessage = OperationMessage(
        updatedAt = updatedAt,
        reason = reason,
        entityId = entityId,
        messageId = messageId,
        threadId = threadId,
        staffId = staffId,
        messageType = messageType,
        instructionId = instructionId,
        reportId = reportId,
        text = text,
        replyTo = replyTo,
        senderName = senderName,
    )
}

internal fun decodeMessageDocumentFields(
    documentId: String,
    optionalString: (Array<out String>) -> String?,
    contains: (String) -> Boolean,
    senderStaffIdFallback: String? = null,
    senderNameFallback: String? = null,
): OperationMessageDecodedFields = OperationMessageDecodedFields(
    updatedAt = optionalString(MESSAGE_UPDATED_AT_FIELDS).orEmpty(),
    reason = optionalString(arrayOf("reason")).orEmpty(),
    entityId = optionalString(MESSAGE_ENTITY_ID_FIELDS) ?: documentId,
    messageId = optionalString(MESSAGE_ID_FIELDS) ?: documentId,
    threadId = requiredString(optionalString, *MESSAGE_THREAD_ID_FIELDS),
    staffId = optionalString(MESSAGE_SENDER_STAFF_ID_FIELDS)
        ?: senderStaffIdFallback
        ?: error("Missing required field from Firestore document: ${MESSAGE_SENDER_STAFF_ID_FIELDS.joinToString()}"),
    messageType = parseMessageType(optionalString, contains),
    instructionId = optionalString(
        arrayOf(
            "instructionId",
            "instruction_id",
            "instruction.instructionId",
            "instruction.instruction_id",
            "payload.instruction.instructionId",
            "payload.instruction.instruction_id",
        )
    ),
    reportId = optionalString(
        arrayOf(
            "reportId",
            "report_id",
            "report.reportId",
            "report.report_id",
            "payload.report.reportId",
            "payload.report.report_id",
        )
    ),
    text = optionalString(arrayOf("text", "simple.text", "payload.simple.text")),
    replyTo = optionalString(
        arrayOf(
            "replyTo",
            "reply_to",
            "simple.replyTo",
            "simple.reply_to",
            "payload.simple.replyTo",
            "payload.simple.reply_to",
        )
    ),
    senderName = optionalString(MESSAGE_SENDER_NAME_FIELDS)
        ?: optionalString(MESSAGE_SENDER_NESTED_NAME_FIELDS)
        ?: senderNameFallback,
)

internal fun OperationMessageType.Companion.parse(raw: String?): OperationMessageType? = when (raw?.uppercase()) {
    "ASSIGN" -> OperationMessageType.ASSIGN
    "UNASSIGN" -> OperationMessageType.UNASSIGN
    "INSTRUCTION" -> OperationMessageType.INSTRUCTION
    "REPORT" -> OperationMessageType.REPORT
    "SIMPLE" -> OperationMessageType.SIMPLE
    else -> null
}

private fun parseMessageType(
    optionalString: (Array<out String>) -> String?,
    contains: (String) -> Boolean,
): OperationMessageType {
    val rawType = optionalString(MESSAGE_TYPE_FIELDS)
    return OperationMessageType.parse(rawType)
        ?: when {
            MESSAGE_SIMPLE_TYPE_FIELDS.any(contains) -> OperationMessageType.SIMPLE
            MESSAGE_ASSIGN_TYPE_FIELDS.any(contains) -> OperationMessageType.ASSIGN
            MESSAGE_UNASSIGN_TYPE_FIELDS.any(contains) -> OperationMessageType.UNASSIGN
            MESSAGE_INSTRUCTION_TYPE_FIELDS.any(contains) -> OperationMessageType.INSTRUCTION
            MESSAGE_REPORT_TYPE_FIELDS.any(contains) -> OperationMessageType.REPORT
            else -> error("Unsupported message document shape")
        }
}

private fun requiredString(
    optionalString: (Array<out String>) -> String?,
    vararg fields: String,
): String = optionalString(fields) ?: error("Missing required field from Firestore document: ${fields.joinToString()}")

private val MESSAGE_UPDATED_AT_FIELDS = arrayOf(
    "updatedAt",
    "updated_at",
    "createdAt",
    "created_at",
    "message.updatedAt",
    "message.updated_at",
    "message.createdAt",
    "message.created_at",
)
private val MESSAGE_ENTITY_ID_FIELDS = arrayOf(
    "entityId",
    "entity_id",
    "message.entityId",
    "message.entity_id",
)
private val MESSAGE_ID_FIELDS = arrayOf(
    "messageId",
    "message_id",
    "message.messageId",
    "message.message_id",
)
private val MESSAGE_THREAD_ID_FIELDS = arrayOf(
    "threadId",
    "thread_id",
    "message.threadId",
    "message.thread_id",
)
private val MESSAGE_SENDER_STAFF_ID_FIELDS = arrayOf(
    "senderStaffId",
    "sender_staff_id",
    "staffId",
    "staff_id",
    "senderId",
    "sender_id",
    "message.senderStaffId",
    "message.sender_staff_id",
    "message.staffId",
    "message.staff_id",
    "payload.senderStaffId",
    "payload.sender_staff_id",
    "payload.staffId",
    "payload.staff_id",
    "sender.staffId",
    "sender.staff_id",
    "sender.id",
)
private val MESSAGE_TYPE_FIELDS = arrayOf(
    "messageType",
    "message_type",
    "message.messageType",
    "message.message_type",
    "payload.messageType",
    "payload.message_type",
)
private val MESSAGE_SENDER_NAME_FIELDS = arrayOf(
    "senderName",
    "sender_name",
    "message.senderName",
    "message.sender_name",
    "payload.senderName",
    "payload.sender_name",
)
private val MESSAGE_SENDER_NESTED_NAME_FIELDS = arrayOf("sender.name")
private val MESSAGE_SIMPLE_TYPE_FIELDS = arrayOf("simple", "payload.simple", "message.simple", "message.payload.simple")
private val MESSAGE_ASSIGN_TYPE_FIELDS = arrayOf("assign", "payload.assign", "message.assign", "message.payload.assign")
private val MESSAGE_UNASSIGN_TYPE_FIELDS = arrayOf(
    "unassign",
    "payload.unassign",
    "message.unassign",
    "message.payload.unassign",
)
private val MESSAGE_INSTRUCTION_TYPE_FIELDS = arrayOf(
    "instruction",
    "payload.instruction",
    "message.instruction",
    "message.payload.instruction",
)
private val MESSAGE_REPORT_TYPE_FIELDS = arrayOf("report", "payload.report", "message.report", "message.payload.report")
