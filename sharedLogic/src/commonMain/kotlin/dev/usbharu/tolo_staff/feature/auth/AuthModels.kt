package dev.usbharu.tolo_staff.feature.auth

import kotlinx.serialization.Serializable

enum class AuthPhase {
    RESTORING,
    SIGNED_OUT,
    AUTHORIZING,
    EXCHANGING,
    AUTHENTICATED,
    ERROR,
}

enum class AuthFailure {
    CANCELLED,
    CONFIGURATION,
    NETWORK,
    AUTHORIZATION,
    TOKEN_EXCHANGE,
    SESSION_EXPIRED,
}

@Serializable
data class AuthTokens(
    val tenantAccessToken: String,
    val tenantExpiresAtEpochSeconds: Long,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val eventAccessToken: String,
    val eventExpiresAtEpochSeconds: Long,
    val subject: String,
)

data class AuthUiState(
    val phase: AuthPhase = AuthPhase.RESTORING,
    val subject: String? = null,
    val failure: AuthFailure? = null,
) {
    val isAuthenticated: Boolean get() = phase == AuthPhase.AUTHENTICATED

    override fun toString(): String = "AuthUiState(phase=$phase, subject=$subject, failure=$failure)"
}
