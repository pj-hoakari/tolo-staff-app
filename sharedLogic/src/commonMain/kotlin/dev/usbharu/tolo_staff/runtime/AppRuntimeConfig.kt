package dev.usbharu.tolo_staff.runtime

enum class FirestoreAccessMode {
    DISABLED,
    AUTHENTICATED,
    DEBUG_EMULATOR_ANONYMOUS,
}

data class AppRuntimeConfig(
    val tenantId: String,
    val eventId: String,
    val staffId: String,
    val grpcHost: String,
    val grpcPort: Int = 8080,
    val firestoreEnabled: Boolean = false,
    val firestoreHost: String = "127.0.0.1",
    val firestorePort: Int = 8081,
    val firestoreProjectId: String = "tolo-communication-poc",
    val firestoreDatabaseId: String? = null,
    val firestoreAccessMode: FirestoreAccessMode = FirestoreAccessMode.DISABLED,
    val firestoreEmulatorEnabled: Boolean = false,
    val firebaseAuthEmulatorEnabled: Boolean = false,
    val firebaseAuthHost: String = "127.0.0.1",
    val firebaseAuthPort: Int = 9099,
    val firebaseApplicationId: String = "",
    val firebaseApiKey: String = "",
    val firebaseGcmSenderId: String = "",
    val isDebugBuild: Boolean = false,
    val oidcLoginEnabled: Boolean = false,
    val oidcIssuer: String = "http://localhost:8080",
    val oidcClientId: String = "tolo-staff-mobile",
    val oidcRedirectUri: String = "dev.usbharu.tolostaff:/oauth2redirect",
    val oidcScopes: String = "openid profile offline_access tenant.read tenant.write events.read events.write",
    val oidcAudience: String = "backend-api",
    val oidcResourceBaseUrl: String = "https://api.example.com",
) {
    fun requireValid(): AppRuntimeConfig = apply {
        require(tenantId.isNotBlank()) { "tenantId must not be blank" }
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(staffId.isNotBlank()) { "staffId must not be blank" }
        require(grpcHost.isNotBlank()) { "grpcHost must not be blank" }
        require(grpcPort in 1..65535) { "grpcPort must be between 1 and 65535" }
        require(firestorePort in 1..65535) { "firestorePort must be between 1 and 65535" }
        require(firebaseAuthPort in 1..65535) { "firebaseAuthPort must be between 1 and 65535" }
        if (firestoreEmulatorEnabled || firebaseAuthEmulatorEnabled) {
            require(isDebugBuild) { "Firebase emulators are only allowed in debug builds" }
        }
        when (firestoreAccessMode) {
            FirestoreAccessMode.DISABLED -> Unit
            FirestoreAccessMode.DEBUG_EMULATOR_ANONYMOUS -> {
                require(isDebugBuild && firestoreEmulatorEnabled) {
                    "Anonymous Firestore access is only allowed with the Firestore emulator in debug builds"
                }
            }
            FirestoreAccessMode.AUTHENTICATED -> {
                require(oidcLoginEnabled) { "Authenticated Firestore access requires OIDC login" }
                require(firebaseApplicationId.isNotBlank()) { "firebaseApplicationId must not be blank" }
                require(firebaseApiKey.isNotBlank()) { "firebaseApiKey must not be blank" }
                require(firebaseGcmSenderId.isNotBlank()) { "firebaseGcmSenderId must not be blank" }
                if (!firebaseAuthEmulatorEnabled) {
                    require(firebaseApiKey != "fake-api-key") {
                        "Authenticated production Firebase access requires a non-placeholder API key"
                    }
                    require(firebaseApplicationId != "1:1234567890:android:tolostaff") {
                        "Authenticated production Firebase access requires a non-placeholder application ID"
                    }
                }
            }
        }
        if (oidcLoginEnabled) {
            require(oidcIssuer.startsWith("https://") || oidcIssuer.startsWith("http://localhost") || oidcIssuer.startsWith("http://10.0.2.2")) {
                "oidcIssuer must use HTTPS outside local development"
            }
            require(oidcClientId.isNotBlank()) { "oidcClientId must not be blank" }
            require(oidcRedirectUri.isNotBlank()) { "oidcRedirectUri must not be blank" }
            require(oidcAudience.isNotBlank()) { "oidcAudience must not be blank" }
            require(oidcResourceBaseUrl.startsWith("https://")) { "oidcResourceBaseUrl must use HTTPS" }
        }
    }

    fun tenantResource(): String = "${oidcResourceBaseUrl.trimEnd('/')}/tenants/$tenantId"

    fun eventResource(): String = "${tenantResource()}/events/$eventId"
}

class AppSessionContext(
    val tenantId: String,
    val eventId: String,
    staffId: String,
) {
    private val _staffId = kotlinx.coroutines.flow.MutableStateFlow(staffId)
    val staffIdFlow: kotlinx.coroutines.flow.StateFlow<String> = _staffId
    val staffId: String get() = _staffId.value

    fun updateStaffId(value: String) {
        require(value.isNotBlank()) { "staffId must not be blank" }
        _staffId.value = value
    }

    override fun equals(other: Any?): Boolean =
        other is AppSessionContext && tenantId == other.tenantId && eventId == other.eventId && staffId == other.staffId

    override fun hashCode(): Int = 31 * (31 * tenantId.hashCode() + eventId.hashCode()) + staffId.hashCode()

    override fun toString(): String = "AppSessionContext(tenantId=$tenantId, eventId=$eventId, staffId=$staffId)"
}

fun AppRuntimeConfig.toSessionContext(): AppSessionContext = AppSessionContext(
    tenantId = tenantId,
    eventId = eventId,
    staffId = staffId,
)

fun interface GrpcMetadataProvider {
    suspend fun metadata(): Map<String, String>
}

object NoOpGrpcMetadataProvider : GrpcMetadataProvider {
    override suspend fun metadata(): Map<String, String> = emptyMap()
}

fun interface RequestIdGenerator {
    fun nextId(): String
}

object DefaultRequestIdGenerator : RequestIdGenerator {
    override fun nextId(): String = buildString {
        append("tolo-")
        append(kotlin.time.Clock.System.now().toEpochMilliseconds())
        append('-')
        append(kotlin.random.Random.nextLong().toString(16))
    }
}
