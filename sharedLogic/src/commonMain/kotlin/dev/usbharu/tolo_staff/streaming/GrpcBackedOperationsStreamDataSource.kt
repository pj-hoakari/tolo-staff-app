package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class OperationsPageConfig(
    val threadPageSize: Int = 20,
    val threadMessagePageSize: Int = 20,
    val assignmentPageSize: Int = 50,
    val instructionPageSize: Int = 50,
)

open class GrpcBackedOperationsStreamDataSource(
    private val remoteDataSource: GrpcOperationsPollingRemoteDataSource,
    private val changeNotifier: OperationsChangeNotifier = NoOpOperationsChangeNotifier(),
    private val pageConfig: OperationsPageConfig = OperationsPageConfig(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : OperationsStreamDataSource {
    private val logger = AppLogger.withTag("GrpcBackedOperationsStreamDataSource")
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)

    private val pointsFlow = MutableStateFlow<List<OperationPoint>>(emptyList())
    private val staffFlow = MutableStateFlow<List<OperationStaff>>(emptyList())
    private val assignmentsFlow = MutableStateFlow<List<OperationAssignment>>(emptyList())
    private val threadsFlow = MutableStateFlow<List<OperationThread>>(emptyList())
    private val messageCacheFlow = MutableStateFlow<Map<String, List<OperationMessage>>>(emptyMap())
    private val relevantInstructionsFlows = mutableMapOf<String, MutableStateFlow<List<OperationInstruction>>>()
    private val staffObserverJobs = mutableMapOf<String, Job>()
    private var globalObserverJob: Job? = null
    private var started = false

    override fun observePoints(): Flow<List<OperationPoint>> {
        start()
        return pointsFlow.asStateFlow()
    }

    override fun observeStaff(): Flow<List<OperationStaff>> {
        start()
        return staffFlow.asStateFlow()
    }

    override fun observeAssignments(): Flow<List<OperationAssignment>> {
        start()
        return assignmentsFlow.asStateFlow()
    }

    override fun observeInstructions(): Flow<List<OperationInstruction>> {
        start()
        return relevantInstructionsFlows.values.firstOrNull()?.asStateFlow()
            ?: MutableStateFlow<List<OperationInstruction>>(emptyList())
    }

    override fun observeRelevantInstructions(currentStaffId: String): Flow<List<OperationInstruction>> {
        start()
        ensureStaffSubscription(currentStaffId)
        return relevantInstructionsFlow(currentStaffId).asStateFlow()
    }

    override fun observeThreads(): Flow<List<OperationThread>> {
        start()
        return threadsFlow.asStateFlow()
    }

    override fun observeMessages(): Flow<List<OperationMessage>> {
        start()
        return messageCacheFlow.asStateFlow()
            .map { cache -> cache.values.flatten().sortedOperationMessages() }
    }

    override fun observeMessages(currentStaffId: String): Flow<List<OperationMessage>> {
        start()
        ensureStaffSubscription(currentStaffId)
        return messageCacheFlow.asStateFlow()
            .map { cache ->
                val visibleThreadIds = threadsFlow.value
                    .asSequence()
                    .filter { currentStaffId in it.members }
                    .map { it.threadId }
                    .toSet()
                cache
                    .filterKeys(visibleThreadIds::contains)
                    .values
                    .flatten()
                    .sortedOperationMessages()
            }
    }

    override fun observeThreadMessages(threadId: String, currentStaffId: String): Flow<List<OperationMessage>> {
        start()
        ensureStaffSubscription(currentStaffId)
        refreshThreadMessages(threadId)
        return messageCacheFlow.asStateFlow()
            .map { cache -> cache[threadId].orEmpty().sortedOperationMessages() }
    }

    override fun start() {
        if (started) {
            return
        }
        started = true
        logger.info { "Starting gRPC-backed operations stream" }
        refreshGlobalData()
        globalObserverJob = scope.launch {
            changeNotifier.observeGlobal().collect { change ->
                logger.debug { "Received global change head: $change" }
                when (change.entityType) {
                    OperationEntityType.POINT -> refreshPoints()
                    OperationEntityType.STAFF -> refreshStaff()
                    OperationEntityType.ASSIGNMENT -> refreshAssignments()
                    else -> Unit
                }
            }
        }
    }

    override fun stop() {
        logger.info { "Stopping gRPC-backed operations stream" }
        globalObserverJob?.cancel()
        globalObserverJob = null
        staffObserverJobs.values.forEach(Job::cancel)
        staffObserverJobs.clear()
        started = false
    }

    private fun ensureStaffSubscription(currentStaffId: String) {
        if (currentStaffId.isBlank() || currentStaffId == UNKNOWN_STAFF_ID) {
            return
        }
        if (staffObserverJobs.containsKey(currentStaffId)) {
            return
        }

        refreshStaffScopedData(currentStaffId)
        staffObserverJobs[currentStaffId] = scope.launch {
            changeNotifier.observeStaff(currentStaffId).collect { change ->
                logger.debug { "Received staff change head: staffId=$currentStaffId, change=$change" }
                when (change.entityType) {
                    OperationEntityType.POINT -> refreshPoints()
                    OperationEntityType.STAFF -> refreshStaff()
                    OperationEntityType.ASSIGNMENT -> refreshAssignments()
                    OperationEntityType.THREAD -> refreshThreadsForStaff(currentStaffId)
                    OperationEntityType.MESSAGE -> {
                        change.threadId?.let(::refreshThreadMessages)
                        refreshThreadsForStaff(currentStaffId, refreshMessages = false)
                    }
                    OperationEntityType.INSTRUCTION -> refreshRelevantInstructions(currentStaffId)
                    OperationEntityType.REPORT,
                    OperationEntityType.UNKNOWN,
                    -> Unit
                }
            }
        }
    }

    private fun refreshGlobalData() {
        scope.launch { refreshPoints() }
        scope.launch { refreshStaff() }
    }

    private fun refreshStaffScopedData(currentStaffId: String) {
        scope.launch { refreshAssignments() }
        scope.launch { refreshRelevantInstructions(currentStaffId) }
        scope.launch { refreshThreadsForStaff(currentStaffId) }
    }

    private suspend fun refreshPoints() {
        pointsFlow.value = remoteDataSource.listPoints()
    }

    private suspend fun refreshStaff() {
        staffFlow.value = remoteDataSource.listStaff()
    }

    private suspend fun refreshAssignments() {
        assignmentsFlow.value = remoteDataSource
            .listAssignmentsPage(pageSize = pageConfig.assignmentPageSize)
            .items
    }

    private suspend fun refreshRelevantInstructions(currentStaffId: String) {
        relevantInstructionsFlow(currentStaffId).value = remoteDataSource
            .listRelevantInstructionsPage(
                staffId = currentStaffId,
                pageSize = pageConfig.instructionPageSize,
            )
            .items
    }

    private suspend fun refreshThreadsForStaff(
        currentStaffId: String,
        refreshMessages: Boolean = true,
    ) {
        val page = remoteDataSource.listThreadsPage(pageSize = pageConfig.threadPageSize)
        threadsFlow.value = page.items
        if (refreshMessages) {
            page.items
                .filter { currentStaffId in it.members }
                .forEach { thread -> refreshThreadMessages(thread.threadId) }
        }
    }

    private fun refreshThreadMessages(threadId: String) {
        scope.launch {
            val page = remoteDataSource.listThreadMessagesPage(
                threadId = threadId,
                pageSize = pageConfig.threadMessagePageSize,
            )
            messageCacheFlow.value = messageCacheFlow.value + (threadId to page.items)
        }
    }

    private fun relevantInstructionsFlow(currentStaffId: String): MutableStateFlow<List<OperationInstruction>> =
        relevantInstructionsFlows.getOrPut(currentStaffId) { MutableStateFlow(emptyList()) }

    private companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
