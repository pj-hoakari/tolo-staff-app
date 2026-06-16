package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    override fun observeMessages(): Flow<List<OperationMessage>> = observeCollection(
        collectionName = MESSAGES_COLLECTION,
        serializer = OperationMessage.serializer(),
        orderField = "messageId",
        primaryOrderField = "updatedAt",
    )

    override fun start() {
        if (!config.enabled || started.value) {
            return
        }

        val firestore = configuredFirestore ?: firestoreFactory().also { created ->
            created.useEmulator(config.host, config.port)
            configuredFirestore = created
        }

        configuredFirestore = firestore
        started.value = true
    }

    override fun stop() {
        started.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observeCollection(
        collectionName: String,
        serializer: KSerializer<T>,
        orderField: String,
        primaryOrderField: String? = null,
    ): Flow<List<T>> = started
        .flatMapLatest { isStarted ->
            if (!config.enabled || !isStarted) {
                flowOf(emptyList())
            } else {
                queryCollection(
                    collectionName = collectionName,
                    primaryOrderField = primaryOrderField,
                    orderField = orderField,
                ).snapshots.map { snapshot ->
                    snapshot.documents.mapNotNull { document ->
                        runCatching { document.data(serializer) }.getOrNull()
                    }
                }
            }
        }
        .onStart { start() }

    private fun queryCollection(
        collectionName: String,
        primaryOrderField: String?,
        orderField: String,
    ) = requireNotNull(configuredFirestore ?: firestoreFactory().also { configuredFirestore = it })
        .collection(collectionName)
        .let { query ->
            val withPrimary = primaryOrderField?.let { query.orderBy(it, Direction.ASCENDING) } ?: query
            withPrimary.orderBy(orderField, Direction.ASCENDING)
        }

    private companion object {
        const val POINTS_COLLECTION = "operations_points"
        const val STAFF_COLLECTION = "operations_staff"
        const val ASSIGNMENTS_COLLECTION = "operations_assignments"
        const val INSTRUCTIONS_COLLECTION = "operations_instructions"
        const val THREADS_COLLECTION = "operations_threads"
        const val MESSAGES_COLLECTION = "operations_messages"
    }
}
