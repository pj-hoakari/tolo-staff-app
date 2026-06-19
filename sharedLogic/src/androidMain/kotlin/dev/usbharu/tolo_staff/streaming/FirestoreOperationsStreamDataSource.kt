package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FirestoreOperationsStreamDataSource(
    private val config: OperationsStreamingConfig = defaultOperationsStreamingConfig(),
    private val firestoreFactory: () -> FirebaseFirestore = {
        Firebase.firestore
    },
) : OperationsStreamDataSource {
    private val logger = AppLogger.withTag("FirestoreOperationsStreamDataSource")
    private val started = MutableStateFlow(false)
    private var configuredFirestore: FirebaseFirestore? = null

    override fun observePoints(): Flow<List<OperationPoint>> = observeCollection(
        collectionName = POINTS_COLLECTION,
        serializer = OperationPoint.serializer(),
        orderField = "pointId",
    )

    override fun observeStaff(): Flow<List<OperationStaff>> = observeCollection(
        collectionName = STAFF_COLLECTION,
        serializer = OperationStaff.serializer(),
        orderField = "staffId",
    )

    override fun observeAssignments(): Flow<List<OperationAssignment>> = observeCollection(
        collectionName = ASSIGNMENTS_COLLECTION,
        serializer = OperationAssignment.serializer(),
        orderField = "assignId",
    )

    override fun observeInstructions(): Flow<List<OperationInstruction>> = observeCollection(
        collectionName = INSTRUCTIONS_COLLECTION,
        serializer = OperationInstruction.serializer(),
        orderField = "instructionId",
    )

    override fun observeThreads(): Flow<List<OperationThread>> = observeCollection(
        collectionName = THREADS_COLLECTION,
        serializer = OperationThread.serializer(),
        orderField = "threadId",
    )

    override fun observeMessages(): Flow<List<OperationMessage>> = flowOf(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMessages(currentStaffId: String): Flow<List<OperationMessage>> = started
        .flatMapLatest { isStarted ->
            if (!config.enabled || !isStarted || currentStaffId.isBlank() || currentStaffId == UNKNOWN_STAFF_ID) {
                flowOf(emptyList())
            } else {
                messageCollection(currentStaffId).snapshots
                    .map { snapshot ->
                        snapshot.documents.mapNotNull(::decodeMessageDocument)
                            .sortedOperationMessages()
                            .also {
                                logger.debug {
                                    "Observed Firestore collection: collectionName=${messageCollectionPath(currentStaffId)}, deserialized=${it.size}, total=${snapshot.documents.size}"
                                }
                            }
                    }
                    .catch { throwable ->
                        logger.warn(throwable) {
                            "Failed to observe Firestore collection: collection=${messageCollectionPath(currentStaffId)}, host=${config.host}, port=${config.port}"
                        }
                        emit(emptyList())
                    }
            }
        }
        .onStart { start() }

    override fun start() {
        if (!config.enabled || started.value) {
            logger.debug {
                "Firestore start skipped: enabled=${config.enabled}, alreadyStarted=${started.value}"
            }
            return
        }

        val firestore = configuredFirestore ?: firestoreFactory().also { created ->
            configuredFirestore = created
        }

        configuredFirestore = firestore
        started.value = true
        logger.info {
            "Firestore operations stream started: enabled=${config.enabled}, host=${config.host}, port=${config.port}"
        }
    }

    override fun stop() {
        started.value = false
        logger.info { "Firestore operations stream stopped" }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observeCollection(
        collectionName: String,
        serializer: KSerializer<T>,
        orderField: String,
        primaryOrderField: String? = null,
        diagnosticFields: List<String> = emptyList(),
    ): Flow<List<T>> = started
        .flatMapLatest { isStarted ->
            if (!config.enabled || !isStarted) {
                flowOf(emptyList())
            } else {
                queryCollection(
                    collectionName = collectionName,
                    primaryOrderField = primaryOrderField,
                    orderField = orderField,
                ).snapshots
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { document ->
                            runCatching { document.data(serializer) }
                                .onFailure { throwable ->
                                    val fieldPresence = if (diagnosticFields.isEmpty()) {
                                        ""
                                    } else {
                                        diagnosticFields.joinToString(
                                            prefix = ", fieldPresence={",
                                            postfix = "}",
                                        ) { field -> "$field=${document.contains(field)}" }
                                    }
                                    logger.warn(throwable) {
                                        "Failed to deserialize Firestore document: collection=$collectionName, documentId=${document.id}$fieldPresence"
                                    }
                                }
                                .getOrNull()
                        }
                            .also {
                                logger.debug {
                                    "Observed Firestore collection: collectionName=$collectionName, deserialized=${it.size}, total=${snapshot.documents.size}"
                                }
                            }
                    }
                    .catch { throwable ->
                        logger.warn(throwable) {
                            "Failed to observe Firestore collection: collection=$collectionName, host=${config.host}, port=${config.port}"
                        }
                        emit(emptyList())
                    }
            }
        }
        .onStart { start() }

    private fun queryCollection(
        collectionName: String,
        primaryOrderField: String?,
        orderField: String,
    ) = requireNotNull(configuredFirestore ?: firestoreFactory().also { configuredFirestore = it })
        .also {
            logger.debug {
                "Creating Firestore query: collection=$collectionName, primaryOrderField=$primaryOrderField, orderField=$orderField"
            }
        }
        .collection(collectionName)
        .let { query ->
            val withPrimary = primaryOrderField?.let { query.orderBy(it, Direction.ASCENDING) } ?: query
            withPrimary.orderBy(orderField, Direction.ASCENDING)
        }

    private fun messageCollection(currentStaffId: String) = requireNotNull(configuredFirestore ?: firestoreFactory().also { configuredFirestore = it })
        .also {
            logger.debug {
                "Creating Firestore query: collection=${messageCollectionPath(currentStaffId)} without explicit ordering"
            }
        }
        .collection(USER_MESSAGES_COLLECTION)
        .document(currentStaffId)
        .collection(MESSAGES_SUBCOLLECTION)

    private fun decodeMessageDocument(document: DocumentSnapshot): OperationMessage? =
        runCatching { document.data(OperationMessage.serializer()) }
            .recoverCatching { decodeMessageDocumentFallback(document) }
            .mapCatching { message -> message.withResolvedSender(decodeSenderEnvelope(document)) }
            .onSuccess { message ->
                val senderEnvelope = decodeSenderEnvelope(document)
                val decodedSenderStaffId = message.normalizedSenderStaffId()
                    ?: senderEnvelope?.resolvedStaffId()
                val decodedSenderName = message.normalizedSenderName()
                    ?: senderEnvelope?.resolvedName()
                logger.trace {
                    "Decoded Firestore message: documentId=${document.id}, messageId=${message.messageId}, threadId=${message.threadId}, senderStaffId=$decodedSenderStaffId, decodedSenderName=$decodedSenderName, messageType=${message.messageType}"
                }
                if (decodedSenderStaffId == null && decodedSenderName == null) {
                    val senderFieldPresence = senderDiagnosticFieldPresence(document, senderEnvelope)
                    logger.warn {
                        "Firestore message is missing sender identity fields: documentId=${document.id}, messageId=${message.messageId}, threadId=${message.threadId}$senderFieldPresence"
                    }
                }
            }
            .onFailure { throwable ->
                val diagnosticFields = listOf(
                    "updatedAt", "updated_at", "reason", "entityId", "entity_id", "messageId", "message_id",
                    "recipientStaffId", "recipient_staff_id", "threadId", "thread_id", "staffId", "staff_id",
                    "senderStaffId", "sender_staff_id", "senderName", "sender_name", "messageType", "message_type",
                    "instructionId", "instruction_id", "reportId", "report_id", "text", "replyTo", "reply_to",
                    "simple", "payload",
                )
                val fieldPresence = diagnosticFields.joinToString(
                    prefix = ", fieldPresence={",
                    postfix = "}",
                ) { field -> "$field=${document.contains(field)}" }
                logger.warn(throwable) {
                    "Failed to deserialize Firestore document: collectionPath=$USER_MESSAGES_COLLECTION/*/$MESSAGES_SUBCOLLECTION, documentId=${document.id}$fieldPresence"
                }
            }
            .getOrNull()

    private fun decodeMessageDocumentFallback(document: DocumentSnapshot): OperationMessage {
        val senderEnvelope = decodeSenderEnvelope(document)
        val fields = decodeMessageDocumentFields(
            documentId = document.id,
            optionalString = { fields -> document.optionalString(*fields) },
            contains = document::contains,
            senderStaffIdFallback = senderEnvelope?.resolvedStaffId(),
            senderNameFallback = senderEnvelope?.resolvedName(),
        )
        logger.debug {
            "Decoded Firestore message via fallback: documentId=${document.id}, messageId=${fields.messageId}, threadId=${fields.threadId}, senderStaffId=${fields.staffId}, senderName=${fields.senderName}, messageType=${fields.messageType}"
        }
        return fields.toOperationMessage()
    }

    private fun parseMessageType(document: DocumentSnapshot): OperationMessageType {
        val rawType = document.optionalString(*MESSAGE_TYPE_FIELDS)
        return OperationMessageType.parse(rawType)
            ?: when {
                MESSAGE_SIMPLE_TYPE_FIELDS.any(document::contains) -> OperationMessageType.SIMPLE
                MESSAGE_ASSIGN_TYPE_FIELDS.any(document::contains) -> OperationMessageType.ASSIGN
                MESSAGE_UNASSIGN_TYPE_FIELDS.any(document::contains) -> OperationMessageType.UNASSIGN
                MESSAGE_INSTRUCTION_TYPE_FIELDS.any(document::contains) -> OperationMessageType.INSTRUCTION
                MESSAGE_REPORT_TYPE_FIELDS.any(document::contains) -> OperationMessageType.REPORT
                else -> error("Unsupported message document shape")
            }
    }

    private fun DocumentSnapshot.optionalString(vararg fields: String): String? =
        fields.firstNotNullOfOrNull { field ->
            runCatching { get<String?>(field) }.getOrNull()
        }?.takeIf { it.isNotBlank() }

    private fun DocumentSnapshot.requiredString(vararg fields: String): String =
        optionalString(*fields) ?: error("Missing required field from Firestore document: ${fields.joinToString()}")

    private fun messageCollectionPath(currentStaffId: String): String =
        "$USER_MESSAGES_COLLECTION/$currentStaffId/$MESSAGES_SUBCOLLECTION"

    private fun senderDiagnosticFieldPresence(
        document: DocumentSnapshot,
        senderEnvelope: FirestoreSenderEnvelope?,
    ): String = (
        MESSAGE_SENDER_STAFF_ID_FIELDS.asList() + MESSAGE_SENDER_NAME_FIELDS.asList() + MESSAGE_SENDER_NESTED_NAME_FIELDS.asList()
        ).distinct()
        .joinToString(
            prefix = ", senderFieldPresence={",
            postfix = "}",
        ) { field -> "$field=${document.contains(field)}" } +
        ", senderEnvelopePresent=${senderEnvelope != null}, senderEnvelopeStaffId=${senderEnvelope?.resolvedStaffId()}, senderEnvelopeName=${senderEnvelope?.resolvedName()}"

    private fun decodeSenderEnvelope(document: DocumentSnapshot): FirestoreSenderEnvelope? =
        runCatching { document.get<FirestoreSenderEnvelope?>("sender") }
            .getOrNull()

    private fun OperationMessage.normalizedSenderStaffId(): String? =
        staffId.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

    private fun OperationMessage.normalizedSenderName(): String? =
        senderName?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

    private fun OperationMessage.withResolvedSender(senderEnvelope: FirestoreSenderEnvelope?): OperationMessage {
        val resolvedStaffId = normalizedSenderStaffId() ?: senderEnvelope?.resolvedStaffId()
        val resolvedSenderName = normalizedSenderName() ?: senderEnvelope?.resolvedName()
        return if (resolvedStaffId == staffId && resolvedSenderName == senderName) {
            this
        } else {
            copy(
                staffId = resolvedStaffId ?: staffId,
                senderName = resolvedSenderName ?: senderName,
            )
        }
    }

    companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
        const val POINTS_COLLECTION = "operations_points"
        const val STAFF_COLLECTION = "operations_staff"
        const val ASSIGNMENTS_COLLECTION = "operations_assignments"
        const val INSTRUCTIONS_COLLECTION = "operations_instructions"
        const val THREADS_COLLECTION = "operations_threads"
        const val USER_MESSAGES_COLLECTION = "operations_user_messages"
        const val MESSAGES_SUBCOLLECTION = "messages"

        val MESSAGE_UPDATED_AT_FIELDS = arrayOf(
            "updatedAt",
            "updated_at",
            "createdAt",
            "created_at",
            "message.updatedAt",
            "message.updated_at",
            "message.createdAt",
            "message.created_at",
        )
        val MESSAGE_ENTITY_ID_FIELDS = arrayOf(
            "entityId",
            "entity_id",
            "message.entityId",
            "message.entity_id",
        )
        val MESSAGE_ID_FIELDS = arrayOf(
            "messageId",
            "message_id",
            "message.messageId",
            "message.message_id",
        )
        val MESSAGE_THREAD_ID_FIELDS = arrayOf(
            "threadId",
            "thread_id",
            "message.threadId",
            "message.thread_id",
        )
        val MESSAGE_SENDER_STAFF_ID_FIELDS = arrayOf(
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
        val MESSAGE_TYPE_FIELDS = arrayOf(
            "messageType",
            "message_type",
            "message.messageType",
            "message.message_type",
            "payload.messageType",
            "payload.message_type",
        )
        val MESSAGE_SENDER_NAME_FIELDS = arrayOf(
            "senderName",
            "sender_name",
            "message.senderName",
            "message.sender_name",
            "payload.senderName",
            "payload.sender_name",
        )
        val MESSAGE_SENDER_NESTED_NAME_FIELDS = arrayOf("sender.name")
        val MESSAGE_SIMPLE_TYPE_FIELDS = arrayOf("simple", "payload.simple", "message.simple", "message.payload.simple")
        val MESSAGE_ASSIGN_TYPE_FIELDS = arrayOf("assign", "payload.assign", "message.assign", "message.payload.assign")
        val MESSAGE_UNASSIGN_TYPE_FIELDS = arrayOf(
            "unassign",
            "payload.unassign",
            "message.unassign",
            "message.payload.unassign",
        )
        val MESSAGE_INSTRUCTION_TYPE_FIELDS = arrayOf(
            "instruction",
            "payload.instruction",
            "message.instruction",
            "message.payload.instruction",
        )
        val MESSAGE_REPORT_TYPE_FIELDS = arrayOf("report", "payload.report", "message.report", "message.payload.report")
    }
}

@Serializable
private data class FirestoreSenderEnvelope(
    val staffId: String? = null,
    @SerialName("staff_id")
    val staffIdSnakeCase: String? = null,
    val id: String? = null,
    val name: String? = null,
) {
    fun resolvedStaffId(): String? = listOf(staffId, staffIdSnakeCase, id)
        .firstOrNull { !it.isNullOrBlank() && !it.equals("null", ignoreCase = true) }

    fun resolvedName(): String? = name
        ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
}

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
    updatedAt = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_UPDATED_AT_FIELDS).orEmpty(),
    reason = optionalString(arrayOf("reason")).orEmpty(),
    entityId = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_ENTITY_ID_FIELDS) ?: documentId,
    messageId = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_ID_FIELDS) ?: documentId,
    threadId = requiredString(optionalString, *FirestoreOperationsStreamDataSource.MESSAGE_THREAD_ID_FIELDS),
    staffId = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_SENDER_STAFF_ID_FIELDS)
        ?: senderStaffIdFallback
        ?: error(
            "Missing required field from Firestore document: ${
                FirestoreOperationsStreamDataSource.MESSAGE_SENDER_STAFF_ID_FIELDS.joinToString()
            }"
        ),
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
    text = optionalString(
        arrayOf(
            "text",
            "simple.text",
            "payload.simple.text",
        )
    ),
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
    senderName = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_SENDER_NAME_FIELDS)
        ?: optionalString(FirestoreOperationsStreamDataSource.MESSAGE_SENDER_NESTED_NAME_FIELDS)
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
    val rawType = optionalString(FirestoreOperationsStreamDataSource.MESSAGE_TYPE_FIELDS)
    return OperationMessageType.parse(rawType)
        ?: when {
            FirestoreOperationsStreamDataSource.MESSAGE_SIMPLE_TYPE_FIELDS.any(contains) -> OperationMessageType.SIMPLE
            FirestoreOperationsStreamDataSource.MESSAGE_ASSIGN_TYPE_FIELDS.any(contains) -> OperationMessageType.ASSIGN
            FirestoreOperationsStreamDataSource.MESSAGE_UNASSIGN_TYPE_FIELDS.any(contains) -> OperationMessageType.UNASSIGN
            FirestoreOperationsStreamDataSource.MESSAGE_INSTRUCTION_TYPE_FIELDS.any(contains) -> OperationMessageType.INSTRUCTION
            FirestoreOperationsStreamDataSource.MESSAGE_REPORT_TYPE_FIELDS.any(contains) -> OperationMessageType.REPORT
            else -> error("Unsupported message document shape")
        }
}

private fun requiredString(
    optionalString: (Array<out String>) -> String?,
    vararg fields: String,
): String = optionalString(fields) ?: error("Missing required field from Firestore document: ${fields.joinToString()}")
