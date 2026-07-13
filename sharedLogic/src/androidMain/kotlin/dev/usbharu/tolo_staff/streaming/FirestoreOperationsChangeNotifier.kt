package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class FirestoreOperationsChangeNotifier(
    private val config: OperationsStreamingConfig = defaultOperationsStreamingConfig(),
    private val session: AppSessionContext,
    private val firestoreFactory: () -> FirebaseFirestore = {
        Firebase.firestore(Firebase.app, config.databaseId)
    },
) : OperationsChangeNotifier {
    private val logger = AppLogger.withTag("FirestoreOperationsChangeNotifier")

    override fun observeGlobal(): Flow<OperationsChangeHead> =
        observeDocument(
            collectionName = GLOBAL_CHANGE_HEADS_COLLECTION,
            documentId = GLOBAL_DOCUMENT_ID,
        )

    override fun observeStaff(staffId: String): Flow<OperationsChangeHead> {
        if (!config.enabled || staffId.isBlank() || staffId == FirestoreOperationsStreamDataSource.UNKNOWN_STAFF_ID) {
            return emptyFlow()
        }
        return observeDocument(
            collectionName = STAFF_CHANGE_HEADS_COLLECTION,
            documentId = staffId,
        )
    }

    private fun observeDocument(
        collectionName: String,
        documentId: String,
    ): Flow<OperationsChangeHead> {
        if (!config.enabled) {
            return emptyFlow()
        }
        val firestore = firestoreFactory()
        var lastVersion = INITIAL_VERSION
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
                    null
                } else {
                    lastVersion = change.version
                    change
                }
            }
            .catch { throwable ->
                logger.warn(throwable) {
                    "Failed to observe change head: collection=$collectionName, documentId=$documentId"
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

    private companion object {
        const val STAFF_CHANGE_HEADS_COLLECTION = "operations_change_heads"
        const val GLOBAL_CHANGE_HEADS_COLLECTION = "operations_global_change_heads"
        const val TENANTS_COLLECTION = "tenants"
        const val EVENTS_COLLECTION = "events"
        const val GLOBAL_DOCUMENT_ID = "global"
        const val INITIAL_VERSION = -1L
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
