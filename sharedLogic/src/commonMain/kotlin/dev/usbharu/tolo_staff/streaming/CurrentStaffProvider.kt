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
)

interface CurrentStaffSession {
    val currentStaff: StateFlow<CurrentStaffMember>
    val isReady: StateFlow<Boolean>

    val currentStaffSnapshot: CurrentStaffMember
        get() = currentStaff.value
}

class FixedCurrentStaffSession(
    private val staffId: String,
    dataSource: OperationsStreamDataSource,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : CurrentStaffSession {
    private val logger = AppLogger.withTag("FixedCurrentStaffSession")
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val _currentStaff = MutableStateFlow(CurrentStaffMember(staffId, staffId))
    private val _isReady = MutableStateFlow(false)

    override val currentStaff: StateFlow<CurrentStaffMember> = _currentStaff.asStateFlow()
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        dataSource.start()
        dataSource.observeStaff()
            .onEach { staff ->
                val current = staff.firstOrNull { it.staffId == staffId }
                _currentStaff.value = CurrentStaffMember(
                    staffId = staffId,
                    displayName = current?.name?.takeIf(String::isNotBlank) ?: staffId,
                )
                _isReady.value = true
            }
            .catch { throwable ->
                logger.warn(throwable) { "Failed to resolve fixed staff: staffId=$staffId" }
                _isReady.value = true
            }
            .launchIn(scope)
    }
}

class MockCurrentStaffSession(
    initialStaff: List<CurrentStaffMember> = listOf(CurrentStaffMember("unknown", "未取得")),
) : CurrentStaffSession {
    private val _currentStaff = MutableStateFlow(initialStaff.firstOrNull() ?: unknownCurrentStaffMember())
    private val _isReady = MutableStateFlow(true)
    override val currentStaff: StateFlow<CurrentStaffMember> = _currentStaff.asStateFlow()
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
}

fun unknownCurrentStaffMember(): CurrentStaffMember = CurrentStaffMember(
    staffId = "unknown",
    displayName = "未取得",
)

fun CurrentStaffMember.isUnknown(): Boolean = staffId == "unknown"
