package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

data class CurrentStaffMember(
    val staffId: String,
    val displayName: String,
    val roleLabel: String? = null,
)

interface CurrentStaffSession {
    val currentStaff: StateFlow<CurrentStaffMember>
    val availableStaff: StateFlow<List<CurrentStaffMember>>
    val isReady: StateFlow<Boolean>

    val currentStaffSnapshot: CurrentStaffMember
        get() = currentStaff.value

    fun selectStaff(staffId: String)
}

class MockCurrentStaffSession(
    private val dataSource: OperationsStreamDataSource? = null,
    initialStaff: List<CurrentStaffMember> = emptyList(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : CurrentStaffSession {
    private val logger = AppLogger.withTag("MockCurrentStaffSession")
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)

    private val initialStaffById = initialStaff.associateBy { it.staffId }
    private val _availableStaff = MutableStateFlow(initialStaff)
    private val _currentStaff = MutableStateFlow(defaultCurrentStaff(initialStaff))
    private val _isReady = MutableStateFlow(initialStaff.isNotEmpty() || dataSource == null)
    private var explicitlySelected = false

    override val currentStaff: StateFlow<CurrentStaffMember> = _currentStaff.asStateFlow()
    override val availableStaff: StateFlow<List<CurrentStaffMember>> = _availableStaff.asStateFlow()
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        logger.info {
            "Initializing current staff session: hasDataSource=${dataSource != null}, initialStaffCount=${initialStaff.size}"
        }
        dataSource?.start()
        dataSource?.observeStaff()
            ?.onEach { streamedStaff ->
                updateAvailableStaff(streamedStaff)
                _isReady.value = true
                logger.debug {
                    "Observed staff update: count=${streamedStaff.size}, currentStaffId=${_currentStaff.value.staffId}"
                }
            }
            ?.catch { throwable ->
                logger.warn(throwable) { "Failed to observe staff; falling back to unknown staff" }
                _availableStaff.value = emptyList()
                _currentStaff.value = unknownCurrentStaffMember()
                _isReady.value = true
            }
            ?.launchIn(scope)
    }

    override fun selectStaff(staffId: String) {
        val nextStaff = _availableStaff.value.firstOrNull { it.staffId == staffId } ?: return
        _currentStaff.value = nextStaff
        explicitlySelected = true
        logger.info { "Selected current staff: staffId=$staffId, displayName=${nextStaff.displayName}" }
    }

    private fun updateAvailableStaff(streamedStaff: List<OperationStaff>) {
        val nextAvailable = streamedStaff.map { it.toCurrentStaffMember() }
        _availableStaff.value = nextAvailable
        val currentStaffId = _currentStaff.value.staffId
        val currentStillAvailable = nextAvailable.firstOrNull { it.staffId == currentStaffId }
        _currentStaff.value = when {
            explicitlySelected && currentStillAvailable != null -> currentStillAvailable
            nextAvailable.any { it.staffId == DEFAULT_STAFF_ID } ->
                nextAvailable.first { it.staffId == DEFAULT_STAFF_ID }
            currentStillAvailable != null -> currentStillAvailable
            else -> defaultCurrentStaff(nextAvailable)
        }
        logger.trace {
            "Updated available staff: count=${nextAvailable.size}, currentStaffId=${_currentStaff.value.staffId}, explicitlySelected=$explicitlySelected"
        }
    }

    private fun OperationStaff.toCurrentStaffMember(): CurrentStaffMember {
        val initial = initialStaffById[staffId]
        return CurrentStaffMember(
            staffId = staffId,
            displayName = name.ifBlank { initial?.displayName ?: staffId },
            roleLabel = roles.firstOrNull() ?: initial?.roleLabel,
        )
    }

    private fun defaultCurrentStaff(candidates: List<CurrentStaffMember>): CurrentStaffMember =
        candidates.firstOrNull { it.staffId == DEFAULT_STAFF_ID }
            ?: candidates.firstOrNull()
            ?: unknownCurrentStaffMember()

    private companion object {
        const val DEFAULT_STAFF_ID = "tanaka"
    }
}

fun unknownCurrentStaffMember(): CurrentStaffMember = CurrentStaffMember(
    staffId = "unknown",
    displayName = "未取得",
    roleLabel = null,
)

fun CurrentStaffMember.isUnknown(): Boolean = staffId == "unknown"
