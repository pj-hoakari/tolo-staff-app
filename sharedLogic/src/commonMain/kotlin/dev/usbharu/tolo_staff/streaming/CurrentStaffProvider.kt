package dev.usbharu.tolo_staff.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val currentStaffSnapshot: CurrentStaffMember
        get() = currentStaff.value

    fun selectStaff(staffId: String)
}

class MockCurrentStaffSession(
    private val dataSource: OperationsStreamDataSource? = null,
    initialStaff: List<CurrentStaffMember> = defaultMockStaffMembers(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : CurrentStaffSession {
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)

    private val fallbackStaff = initialStaff.ifEmpty { defaultMockStaffMembers() }
    private val _availableStaff = MutableStateFlow(fallbackStaff)
    private val _currentStaff = MutableStateFlow(defaultCurrentStaff(fallbackStaff))

    override val currentStaff: StateFlow<CurrentStaffMember> = _currentStaff.asStateFlow()
    override val availableStaff: StateFlow<List<CurrentStaffMember>> = _availableStaff.asStateFlow()

    init {
        dataSource?.start()
        dataSource?.observeStaff()
            ?.onEach { streamedStaff -> updateAvailableStaff(streamedStaff) }
            ?.launchIn(scope)
    }

    override fun selectStaff(staffId: String) {
        val nextStaff = _availableStaff.value.firstOrNull { it.staffId == staffId } ?: return
        _currentStaff.value = nextStaff
    }

    private fun updateAvailableStaff(streamedStaff: List<OperationStaff>) {
        val nextAvailable = streamedStaff.map { it.toCurrentStaffMember() }.ifEmpty { fallbackStaff }
        _availableStaff.value = nextAvailable
        _currentStaff.value = nextAvailable.firstOrNull { it.staffId == _currentStaff.value.staffId }
            ?: defaultCurrentStaff(nextAvailable)
    }

    private fun OperationStaff.toCurrentStaffMember(): CurrentStaffMember {
        val fallback = fallbackStaff.firstOrNull { it.staffId == staffId }
        return CurrentStaffMember(
            staffId = staffId,
            displayName = if (name.isBlank()) fallback?.displayName ?: staffId else name,
            roleLabel = roles.firstOrNull() ?: fallback?.roleLabel,
        )
    }

    private fun defaultCurrentStaff(candidates: List<CurrentStaffMember>): CurrentStaffMember =
        candidates.firstOrNull { it.staffId == DEFAULT_STAFF_ID } ?: candidates.first()

    private companion object {
        const val DEFAULT_STAFF_ID = "tanaka"
    }
}

fun defaultMockStaffMembers(): List<CurrentStaffMember> = listOf(
    CurrentStaffMember(
        staffId = "tanaka",
        displayName = "田中",
        roleLabel = "Aゲート担当",
    ),
    CurrentStaffMember(
        staffId = "sato",
        displayName = "佐藤",
        roleLabel = "巡回担当",
    ),
    CurrentStaffMember(
        staffId = "yamada",
        displayName = "山田",
        roleLabel = "サブリーダー",
    ),
)
