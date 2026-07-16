package dev.usbharu.tolo_staff.feature.auth

import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Clock

class AuthRepository(
    private val sessionContext: AppSessionContext,
) {
    private val logger = AppLogger.withTag("AuthRepository")
    private val _state = MutableStateFlow(AuthUiState())
    private val _revision = MutableStateFlow(0L)
    private val refreshRequestChannel = Channel<AuthRefreshReason>(Channel.CONFLATED)
    private var tokens: AuthTokens? = null

    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    val revision: StateFlow<Long> = _revision.asStateFlow()
    val refreshRequests: Flow<AuthRefreshReason> = refreshRequestChannel.receiveAsFlow()

    fun restore(
        restoredTokens: AuthTokens?,
        nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    ) {
        logger.info { "OIDC session restoration started: tokenPresent=${restoredTokens != null}" }
        if (restoredTokens == null || restoredTokens.eventExpiresAtEpochSeconds <= nowEpochSeconds) {
            tokens = null
            _state.value = AuthUiState(AuthPhase.SIGNED_OUT)
            logger.info { "OIDC session restoration completed: authenticated=false" }
            return
        }
        authenticate(restoredTokens)
        logger.info { "OIDC session restoration completed: authenticated=true" }
    }

    fun startAuthorization() {
        logger.info { "OIDC authorization started" }
        _state.value = AuthUiState(AuthPhase.AUTHORIZING)
    }

    fun startTokenExchange() {
        logger.info { "OIDC event token exchange started" }
        _state.value = AuthUiState(AuthPhase.EXCHANGING)
    }

    fun authenticate(newTokens: AuthTokens) {
        require(newTokens.subject.isNotBlank()) { "OIDC subject must not be blank" }
        tokens = newTokens
        sessionContext.updateStaffId(newTokens.subject)
        _revision.value += 1
        _state.value = AuthUiState(AuthPhase.AUTHENTICATED, subject = newTokens.subject)
        logger.info { "OIDC authentication completed: subject=${newTokens.subject}" }
    }

    fun fail(failure: AuthFailure) {
        if (failure == AuthFailure.SESSION_EXPIRED) tokens = null
        val phase = if (failure == AuthFailure.CANCELLED || failure == AuthFailure.SESSION_EXPIRED) {
            AuthPhase.SIGNED_OUT
        } else {
            AuthPhase.ERROR
        }
        _state.value = AuthUiState(phase = phase, failure = failure)
        logger.warn { "OIDC authentication failed: reason=$failure" }
    }

    fun logout() {
        logger.info { "OIDC logout started" }
        tokens = null
        _revision.value += 1
        _state.value = AuthUiState(AuthPhase.SIGNED_OUT)
        logger.info { "OIDC logout completed" }
    }

    fun currentTokens(): AuthTokens? = tokens

    fun requestRefresh(reason: AuthRefreshReason): Boolean {
        logger.info { "OIDC refresh requested: reason=$reason" }
        return refreshRequestChannel.trySend(reason).isSuccess
    }

    fun validEventAccessToken(nowEpochSeconds: Long = Clock.System.now().epochSeconds): String? =
        tokens?.takeIf { it.eventExpiresAtEpochSeconds > nowEpochSeconds }?.eventAccessToken

    fun needsRefresh(
        nowEpochSeconds: Long = Clock.System.now().epochSeconds,
        leewaySeconds: Long = 60,
    ): Boolean = tokens?.eventExpiresAtEpochSeconds?.let { it <= nowEpochSeconds + leewaySeconds } ?: false
}
