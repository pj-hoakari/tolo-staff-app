package dev.usbharu.tolo_staff.feature.auth

import dev.usbharu.tolo_staff.runtime.GrpcMetadataProvider

class AuthGrpcMetadataProvider(
    private val repository: AuthRepository,
) : GrpcMetadataProvider {
    override suspend fun metadata(): Map<String, String> {
        val token = repository.validEventAccessToken()
        if (token == null && repository.currentTokens() != null) {
            repository.fail(AuthFailure.SESSION_EXPIRED)
        }
        return token?.let { mapOf("authorization" to "Bearer $it") }.orEmpty()
    }
}
