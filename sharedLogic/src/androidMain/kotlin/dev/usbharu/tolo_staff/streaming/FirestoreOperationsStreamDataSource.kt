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
            .onFailure { throwable ->
                val diagnosticFields = listOf(
                    "updatedAt", "updated_at", "reason", "entityId", "entity_id", "messageId", "message_id",
                    "recipientStaffId", "recipient_staff_id", "threadId", "thread_id", "staffId", "staff_id",
                    "senderStaffId", "sender_staff_id", "messageType", "message_type",
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
        val fields = OperationMessageDecodedFields(
            updatedAt = document.optionalString("updatedAt", "updated_at").orEmpty(),
            reason = document.optionalString("reason").orEmpty(),
            entityId = document.optionalString("entityId", "entity_id") ?: document.id,
            messageId = document.optionalString("messageId", "message_id") ?: document.id,
            threadId = document.requiredString("threadId", "thread_id"),
            staffId = document.requiredString("senderStaffId", "sender_staff_id", "staffId", "staff_id"),
            messageType = parseMessageType(document),
            instructionId = document.optionalString(
                "instructionId",
                "instruction_id",
                "instruction.instructionId",
                "instruction.instruction_id",
                "payload.instruction.instructionId",
                "payload.instruction.instruction_id",
            ),
            reportId = document.optionalString(
                "reportId",
                "report_id",
                "report.reportId",
                "report.report_id",
                "payload.report.reportId",
                "payload.report.report_id",
            ),
            text = document.optionalString(
                "text",
                "simple.text",
                "payload.simple.text",
            ),
            replyTo = document.optionalString(
                "replyTo",
                "reply_to",
                "simple.replyTo",
                "simple.reply_to",
                "payload.simple.replyTo",
                "payload.simple.reply_to",
            ),
        )
        return fields.toOperationMessage()
    }

    private fun parseMessageType(document: DocumentSnapshot): OperationMessageType {
        val rawType = document.optionalString("messageType", "message_type")
        return OperationMessageType.parse(rawType)
            ?: when {
                document.contains("simple") || document.contains("payload.simple") -> OperationMessageType.SIMPLE
                document.contains("assign") || document.contains("payload.assign") -> OperationMessageType.ASSIGN
                document.contains("unassign") || document.contains("payload.unassign") -> OperationMessageType.UNASSIGN
                document.contains("instruction") || document.contains("payload.instruction") -> OperationMessageType.INSTRUCTION
                document.contains("report") || document.contains("payload.report") -> OperationMessageType.REPORT
                else -> error("Unsupported message document shape")
            }
    }

    private fun DocumentSnapshot.optionalString(vararg fields: String): String? =
        fields.firstNotNullOfOrNull { field ->
            runCatching {
                if (!contains(field)) {
                    null
                } else {
                    get<String?>(field)
                }
            }.getOrNull()
        }?.takeIf { it.isNotBlank() }

    private fun DocumentSnapshot.requiredString(vararg fields: String): String =
        optionalString(*fields) ?: error("Missing required field from Firestore document: ${fields.joinToString()}")

    private fun messageCollectionPath(currentStaffId: String): String =
        "$USER_MESSAGES_COLLECTION/$currentStaffId/$MESSAGES_SUBCOLLECTION"

    private companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
        const val POINTS_COLLECTION = "operations_points"
        const val STAFF_COLLECTION = "operations_staff"
        const val ASSIGNMENTS_COLLECTION = "operations_assignments"
        const val INSTRUCTIONS_COLLECTION = "operations_instructions"
        const val THREADS_COLLECTION = "operations_threads"
        const val USER_MESSAGES_COLLECTION = "operations_user_messages"
        const val MESSAGES_SUBCOLLECTION = "messages"
    }
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
    )
}

internal fun OperationMessageType.Companion.parse(raw: String?): OperationMessageType? = when (raw?.uppercase()) {
    "ASSIGN" -> OperationMessageType.ASSIGN
    "UNASSIGN" -> OperationMessageType.UNASSIGN
    "INSTRUCTION" -> OperationMessageType.INSTRUCTION
    "REPORT" -> OperationMessageType.REPORT
    "SIMPLE" -> OperationMessageType.SIMPLE
    else -> null
}
