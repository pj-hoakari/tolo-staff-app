package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.flow.Flow

class PollingOperationsStreamDataSource(
    private val remoteDataSource: OperationsPollingRemoteDataSource,
    private val config: OperationsPollingConfig = defaultOperationsPollingConfig(),
) : OperationsStreamDataSource {
    private val logger = AppLogger.withTag("PollingOperationsStreamDataSource")
    private val pollingFactory = SharedPollingFlowFactory(config.intervalMillis)
    private val pointsFlow by lazy { pollingFactory.create { remoteDataSource.listPoints() } }
    private val staffFlow by lazy { pollingFactory.create { remoteDataSource.listStaff() } }
    private val assignmentsFlow by lazy { pollingFactory.create { remoteDataSource.listAssignments() } }
    private val instructionsFlow by lazy { pollingFactory.create { remoteDataSource.listInstructions() } }
    private val threadsFlow by lazy { pollingFactory.create { remoteDataSource.listThreads() } }
    private val messagesFlow by lazy {
        pollingFactory.create {
            remoteDataSource.listMessages().sortedOperationMessages()
        }
    }
    private val relevantInstructionsFlows = mutableMapOf<String, Flow<List<OperationInstruction>>>()

    override fun observePoints(): Flow<List<OperationPoint>> = pointsFlow

    override fun observeStaff(): Flow<List<OperationStaff>> = staffFlow

    override fun observeAssignments(): Flow<List<OperationAssignment>> = assignmentsFlow

    override fun observeInstructions(): Flow<List<OperationInstruction>> = instructionsFlow

    override fun observeRelevantInstructions(currentStaffId: String): Flow<List<OperationInstruction>> =
        relevantInstructionsFlows.getOrPut(currentStaffId) {
            logger.info { "Creating relevant instructions flow: currentStaffId=$currentStaffId" }
            pollingFactory.create { remoteDataSource.listRelevantInstructions(currentStaffId) }
        }

    override fun observeThreads(): Flow<List<OperationThread>> = threadsFlow

    override fun observeMessages(): Flow<List<OperationMessage>> = messagesFlow

    override fun start() {
        logger.info { "Polling operations stream started: intervalMillis=${config.intervalMillis}" }
    }

    override fun stop() {
        logger.info { "Polling operations stream stopped" }
    }
}

fun List<OperationMessage>.sortedOperationMessages(): List<OperationMessage> =
    sortedWith(compareBy<OperationMessage> { it.updatedAt }.thenBy { it.messageId })
