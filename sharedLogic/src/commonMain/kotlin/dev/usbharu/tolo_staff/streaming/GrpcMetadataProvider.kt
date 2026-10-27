package dev.usbharu.tolo_staff.streaming

/**
 * Supplies gRPC metadata such as the `authorization` header.
 *
 * Authentication is implemented outside of the networking layer so that the client can be
 * constructed before an OIDC session is restored.
 */
fun interface GrpcMetadataProvider {
    suspend fun metadata(): Map<String, String>
}

object NoOpGrpcMetadataProvider : GrpcMetadataProvider {
    override suspend fun metadata(): Map<String, String> = emptyMap()
}
