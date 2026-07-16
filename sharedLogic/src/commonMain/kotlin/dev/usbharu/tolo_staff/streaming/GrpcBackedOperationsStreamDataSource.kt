package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.CancellationException
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

open class GrpcBackedOperationsStreamDataSource(
    private val remoteDataSource: OperationsPollingRemoteDataSource,
    private val changeNotifier: OperationsChangeNotifier = NoOpOperationsChangeNotifier(),
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
    private var resyncObserverJob: Job? = null
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
                    OperationEntityType.UNKNOWN -> {
                        refreshGlobalData()
                        staffObserverJobs.keys.forEach(::refreshStaffScopedData)
                    }
                    else -> Unit
                }
            }
        }
        resyncObserverJob = scope.launch {
            changeNotifier.observeResyncRequests().collect {
                logger.info { "Refreshing gRPC snapshots after Firestore listener session change" }
                refreshGlobalData()
                staffObserverJobs.keys.forEach(::refreshStaffScopedData)
            }
        }
    }

    override fun stop() {
        logger.info { "Stopping gRPC-backed operations stream" }
        globalObserverJob?.cancel()
        globalObserverJob = null
        resyncObserverJob?.cancel()
        resyncObserverJob = null
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
                    OperationEntityType.UNKNOWN -> {
                        refreshGlobalData()
                        refreshStaffScopedData(currentStaffId)
                    }
                    OperationEntityType.REPORT -> Unit
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
        refreshSafely("points") {
            pointsFlow.value = remoteDataSource.listPoints()
        }
    }

    private suspend fun refreshStaff() {
        refreshSafely("staff") {
            staffFlow.value = remoteDataSource.listStaff()
        }
    }

    private suspend fun refreshAssignments() {
        refreshSafely("assignments") {
            assignmentsFlow.value = remoteDataSource.listAssignments()
        }
    }

    private suspend fun refreshRelevantInstructions(currentStaffId: String) {
        refreshSafely("instructions for staffId=$currentStaffId") {
            relevantInstructionsFlow(currentStaffId).value = remoteDataSource.listRelevantInstructions(currentStaffId)
        }
    }

    private suspend fun refreshThreadsForStaff(
        currentStaffId: String,
        refreshMessages: Boolean = true,
    ) {
        refreshSafely("threads for staffId=$currentStaffId") {
            val threads = remoteDataSource.listThreads()
            threadsFlow.value = threads
            if (refreshMessages) {
                threads
                    .filter { currentStaffId in it.members }
                    .forEach { thread -> refreshThreadMessages(thread.threadId) }
            }
        }
    }

    private fun refreshThreadMessages(threadId: String) {
        scope.launch {
            refreshSafely("messages for threadId=$threadId") {
                val messages = remoteDataSource.listThreadMessages(threadId)
                messageCacheFlow.value = messageCacheFlow.value + (threadId to messages)
            }
        }
    }

    private suspend fun refreshSafely(
        target: String,
        refresh: suspend () -> Unit,
    ) {
        try {
            refresh()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.warn(throwable) {
                "Failed to refresh $target; keeping the last available data"
            }
        }
    }

    private fun relevantInstructionsFlow(currentStaffId: String): MutableStateFlow<List<OperationInstruction>> =
        relevantInstructionsFlows.getOrPut(currentStaffId) { MutableStateFlow(emptyList()) }

    private companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
