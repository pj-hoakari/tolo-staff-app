package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreOperationsChangeNotifier(
    private val config: OperationsStreamingConfig = defaultOperationsStreamingConfig(),
    private val session: AppSessionContext,
    private val firebaseSessionController: FirebaseSessionController,
    private val firestoreFactory: () -> FirebaseFirestore = {
        Firebase.firestore(Firebase.app, config.databaseId)
    },
) : OperationsChangeNotifier {
    private val logger = AppLogger.withTag("FirestoreOperationsChangeNotifier")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val staffFlows = mutableMapOf<String, Flow<OperationsChangeHead>>()

    private val globalFlow: Flow<OperationsChangeHead> by lazy {
        observeDocument(
            collectionName = GLOBAL_CHANGE_HEADS_COLLECTION,
            documentId = GLOBAL_DOCUMENT_ID,
            requiredStaffId = null,
        ).shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 0)
    }

    override fun observeGlobal(): Flow<OperationsChangeHead> =
        if (config.enabled) globalFlow else emptyFlow()

    override fun observeStaff(staffId: String): Flow<OperationsChangeHead> {
        if (!config.enabled || staffId.isBlank() || staffId == FirestoreOperationsStreamDataSource.UNKNOWN_STAFF_ID) {
            return emptyFlow()
        }
        return synchronized(staffFlows) {
            staffFlows.getOrPut(staffId) {
                observeDocument(
                    collectionName = STAFF_CHANGE_HEADS_COLLECTION,
                    documentId = staffId,
                    requiredStaffId = staffId,
                ).shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 0)
            }
        }
    }

    override fun observeResyncRequests(): Flow<Unit> =
        firebaseSessionController.state
            .mapNotNull { state -> state.takeIf(FirebaseSessionState::isReady)?.generation }
            .distinctUntilChanged()
            .map { Unit }

    private fun observeDocument(
        collectionName: String,
        documentId: String,
        requiredStaffId: String?,
    ): Flow<OperationsChangeHead> =
        firebaseSessionController.state
            .map { state ->
                state.takeIf {
                    it.isReady &&
                        it.tenantId == session.tenantId &&
                        it.eventId == session.eventId &&
                        (requiredStaffId == null || it.staffId == requiredStaffId)
                }?.generation
            }
            .distinctUntilChanged()
            .flatMapLatest { generation ->
                if (generation == null) {
                    emptyFlow()
                } else {
                    observeReadyDocument(collectionName, documentId, generation)
                }
            }

    private fun observeReadyDocument(
        collectionName: String,
        documentId: String,
        generation: Long,
    ): Flow<OperationsChangeHead> {
        var lastVersion = INITIAL_VERSION
        val firestore = firestoreFactory()
        logger.info {
            "Starting Firestore change-head listener: collection=$collectionName, documentId=$documentId, generation=$generation"
        }
        return firestore.collection(TENANTS_COLLECTION)
            .document(session.tenantId)
            .collection(EVENTS_COLLECTION)
            .document(session.eventId)
            .collection(collectionName)
            .document(documentId)
            .snapshots
            .map { snapshot -> snapshot.toOperationsChangeHead() }
            .mapNotNull { change ->
                if (change.version <= lastVersion) {
                    logger.debug {
                        "Ignored duplicate change head: collection=$collectionName, documentId=$documentId, " +
                            "version=${change.version}, lastVersion=$lastVersion"
                    }
                    null
                } else {
                    lastVersion = change.version
                    change
                }
            }
            .retryWhen { throwable, attempt ->
                if (throwable.isPermissionDenied()) {
                    logger.warn(throwable) {
                        "Firestore permission denied: collection=$collectionName, documentId=$documentId, generation=$generation"
                    }
                    firebaseSessionController.recoverFromPermissionDenied()
                    false
                } else if (throwable.isTransientListenerFailure()) {
                    val backoff = min(MAX_RETRY_MILLIS, BASE_RETRY_MILLIS shl attempt.coerceAtMost(4).toInt())
                    logger.debug {
                        "Retrying Firestore listener: collection=$collectionName, documentId=$documentId, " +
                            "attempt=${attempt + 1}, delayMillis=$backoff"
                    }
                    delay(backoff)
                    true
                } else {
                    false
                }
            }
            .catch { throwable ->
                logger.warn(throwable) {
                    "Firestore listener stopped: collection=$collectionName, documentId=$documentId, generation=$generation"
                }
            }
    }

    private fun DocumentSnapshot.toOperationsChangeHead(): OperationsChangeHead = OperationsChangeHead(
        version = longValue("version"),
        updatedAt = stringValue("updatedAt", "updated_at"),
        entityType = OperationEntityType.parse(stringValue("entityType", "entity_type")),
        entityId = stringValue("entityId", "entity_id"),
        reason = stringValue("reason"),
        threadId = stringValue("threadId", "thread_id").ifBlank { null },
        operation = OperationChangeType.parse(stringValue("operation")),
    )

    private fun DocumentSnapshot.stringValue(vararg fieldNames: String): String =
        fieldNames.firstNotNullOfOrNull { fieldName ->
            runCatching { get<String?>(fieldName) }.getOrNull()
        }.orEmpty()

    private fun DocumentSnapshot.longValue(fieldName: String): Long =
        runCatching { get<Long?>(fieldName) }
            .getOrNull()
            ?: runCatching { get<Double?>(fieldName)?.toLong() }.getOrNull()
            ?: 0L

    private fun Throwable.isPermissionDenied(): Boolean =
        "${this::class.simpleName}:$message".contains("PERMISSION_DENIED", ignoreCase = true)

    private fun Throwable.isTransientListenerFailure(): Boolean {
        val value = "${this::class.simpleName}:$message".uppercase()
        return TRANSIENT_FAILURE_NAMES.any(value::contains)
    }

    private companion object {
        const val STAFF_CHANGE_HEADS_COLLECTION = "operations_change_heads"
        const val GLOBAL_CHANGE_HEADS_COLLECTION = "operations_global_change_heads"
        const val TENANTS_COLLECTION = "tenants"
        const val EVENTS_COLLECTION = "events"
        const val GLOBAL_DOCUMENT_ID = "global"
        const val INITIAL_VERSION = -1L
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val BASE_RETRY_MILLIS = 500L
        const val MAX_RETRY_MILLIS = 10_000L
        val TRANSIENT_FAILURE_NAMES = listOf(
            "UNAVAILABLE",
            "DEADLINE_EXCEEDED",
            "RESOURCE_EXHAUSTED",
            "ABORTED",
            "INTERNAL",
            "NETWORK",
            "IOEXCEPTION",
        )
    }
}

internal fun eventScopedChangeHeadPath(
    session: AppSessionContext,
    collectionName: String,
    documentId: String,
): String = "tenants/${session.tenantId}/events/${session.eventId}/$collectionName/$documentId"

private fun OperationEntityType.Companion.parse(rawValue: String): OperationEntityType =
    when (rawValue.uppercase()) {
        "POINT" -> OperationEntityType.POINT
        "STAFF" -> OperationEntityType.STAFF
        "ASSIGNMENT" -> OperationEntityType.ASSIGNMENT
        "THREAD" -> OperationEntityType.THREAD
        "MESSAGE" -> OperationEntityType.MESSAGE
        "INSTRUCTION" -> OperationEntityType.INSTRUCTION
        "REPORT" -> OperationEntityType.REPORT
        else -> OperationEntityType.UNKNOWN
    }

private fun OperationChangeType.Companion.parse(rawValue: String): OperationChangeType =
    when (rawValue.uppercase()) {
        "CREATED" -> OperationChangeType.CREATED
        "UPDATED" -> OperationChangeType.UPDATED
        "DELETED" -> OperationChangeType.DELETED
        else -> OperationChangeType.UNKNOWN
    }
