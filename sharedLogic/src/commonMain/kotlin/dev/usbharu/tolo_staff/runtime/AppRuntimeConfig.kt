package dev.usbharu.tolo_staff.runtime

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
) {
    fun requireValid(): AppRuntimeConfig = apply {
        require(tenantId.isNotBlank()) { "tenantId must not be blank" }
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(staffId.isNotBlank()) { "staffId must not be blank" }
        require(grpcHost.isNotBlank()) { "grpcHost must not be blank" }
        require(grpcPort in 1..65535) { "grpcPort must be between 1 and 65535" }
    }
}

data class AppSessionContext(
    val tenantId: String,
    val eventId: String,
    val staffId: String,
)

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
