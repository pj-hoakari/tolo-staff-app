package dev.usbharu.tolo_staff.feature.auth

import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AuthRepositoryTest {
    private val session = AppSessionContext("tenant-a", "event-1", "fallback")
    private val repository = AuthRepository(session)

    @Test
    fun `restoring valid tokens authenticates and updates staff subject`() {
        repository.restore(validTokens().copy(eventExpiresAtEpochSeconds = Long.MAX_VALUE), nowEpochSeconds = 100)

        assertEquals(AuthPhase.AUTHENTICATED, repository.state.value.phase)
        assertEquals("staff-1", repository.state.value.subject)
        assertEquals("staff-1", session.staffId)
        assertEquals("event-token", repository.validEventAccessToken(nowEpochSeconds = 100))
    }

    @Test
    fun `expired restored tokens require sign in`() {
        repository.restore(validTokens().copy(eventExpiresAtEpochSeconds = 99), nowEpochSeconds = 100)

        assertEquals(AuthPhase.SIGNED_OUT, repository.state.value.phase)
        assertNull(repository.validEventAccessToken(nowEpochSeconds = 100))
    }

    @Test
    fun `authorization and exchange phases are observable`() {
        repository.startAuthorization()
        assertEquals(AuthPhase.AUTHORIZING, repository.state.value.phase)

        repository.startTokenExchange()
        assertEquals(AuthPhase.EXCHANGING, repository.state.value.phase)
    }

    @Test
    fun `logout removes tokens and returns to signed out`() {
        repository.restore(validTokens(), nowEpochSeconds = 100)
        repository.logout()

        assertEquals(AuthPhase.SIGNED_OUT, repository.state.value.phase)
        assertNull(repository.currentTokens())
        assertFalse(repository.state.value.isAuthenticated)
    }

    @Test
    fun `refresh threshold is reported without exposing token values`() {
        repository.restore(validTokens().copy(eventExpiresAtEpochSeconds = 150), nowEpochSeconds = 100)

        assertTrue(repository.needsRefresh(nowEpochSeconds = 100, leewaySeconds = 60))
        assertFalse(repository.state.value.toString().contains("event-token"))
    }

    @Test
    fun `grpc metadata contains only the event bearer token`() = runTest {
        repository.restore(validTokens().copy(eventExpiresAtEpochSeconds = Long.MAX_VALUE), nowEpochSeconds = 100)

        val metadata = AuthGrpcMetadataProvider(repository).metadata()

        assertEquals(mapOf("authorization" to "Bearer event-token"), metadata)
        assertFalse(metadata.toString().contains("tenant-token"))
        assertFalse(metadata.toString().contains("refresh-token"))
    }

    @Test
    fun `unrecoverable session expiry removes tokens and requires login`() {
        repository.restore(validTokens(), nowEpochSeconds = 100)

        repository.fail(AuthFailure.SESSION_EXPIRED)

        assertEquals(AuthPhase.SIGNED_OUT, repository.state.value.phase)
        assertNull(repository.currentTokens())
    }
}

internal fun validTokens() = AuthTokens(
    tenantAccessToken = "tenant-token",
    tenantExpiresAtEpochSeconds = 500,
    refreshToken = "refresh-token",
    idToken = "id-token",
    eventAccessToken = "event-token",
    eventExpiresAtEpochSeconds = 400,
    subject = "staff-1",
)
