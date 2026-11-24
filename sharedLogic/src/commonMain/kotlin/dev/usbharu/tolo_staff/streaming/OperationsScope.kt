package dev.usbharu.tolo_staff.streaming

data class OperationsScope(
    val eventId: String = "",
    val tenantId: String = "",
    val accessToken: String = "",
)

fun OperationsScope.grpcMetadataProvider(): GrpcMetadataProvider =
    if (accessToken.isBlank()) {
        NoOpGrpcMetadataProvider
    } else {
        GrpcMetadataProvider { mapOf("authorization" to "Bearer $accessToken") }
    }
