package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.feature.auth.AuthRefreshReason
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.feature.auth.validTokens
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.FirestoreAccessMode
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseSessionControllerTest {
    @Test
    fun `authenticated session exchanges signs in and becomes ready`() = runTest {
        val fixture = fixture()

        fixture.authRepository.authenticate(validTokens())
        runCurrent()

        assertEquals(FirebaseSessionPhase.READY, fixture.controller.state.value.phase)
        assertEquals(1, fixture.broker.exchangeCount)
        assertEquals("firebase-user", fixture.controller.state.value.firebaseUid)
        assertFalse(fixture.controller.state.value.toString().contains("custom-secret"))
        assertFalse(fixture.broker.lastGrant.toString().contains("custom-secret"))
    }

    @Test
    fun `signed out session does not exchange`() = runTest {
        val fixture = fixture()

        runCurrent()

        assertEquals(FirebaseSessionPhase.SIGNED_OUT, fixture.controller.state.value.phase)
        assertEquals(0, fixture.broker.exchangeCount)
    }

    @Test
    fun `uid mismatch signs out and reports configuration error`() = runTest {
        val fixture = fixture(gatewayUid = "different-user")

        fixture.authRepository.authenticate(validTokens())
        runCurrent()

        assertEquals(FirebaseSessionPhase.ERROR, fixture.controller.state.value.phase)
        assertEquals(FirebaseSessionFailure.CONFIGURATION, fixture.controller.state.value.failure)
        assertTrue(fixture.gateway.signOutCount > 0)
    }

    @Test
    fun `sign in failure obtains a fresh custom token once`() = runTest {
        val fixture = fixture(signInFailures = 1)

        fixture.authRepository.authenticate(validTokens())
        runCurrent()

        assertEquals(FirebaseSessionPhase.READY, fixture.controller.state.value.phase)
        assertEquals(2, fixture.broker.exchangeCount)
        assertEquals(2, fixture.gateway.signInCount)
    }

    @Test
    fun `grant refresh leaves ready and requests oidc refresh`() = runTest {
        val fixture = fixture(grantExpiresAt = 1_120)
        val request = async { fixture.authRepository.refreshRequests.first() }

        fixture.authRepository.authenticate(validTokens())
        runCurrent()
        assertEquals(FirebaseSessionPhase.READY, fixture.controller.state.value.phase)

        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(FirebaseSessionPhase.REFRESHING, fixture.controller.state.value.phase)
        assertEquals(AuthRefreshReason.FIREBASE_GRANT_EXPIRING, request.await())
    }

    @Test
    fun `debug emulator anonymous mode becomes ready without exchange`() = runTest {
        val fixture = fixture(
            config = baseConfig().copy(
                oidcLoginEnabled = false,
                firestoreAccessMode = FirestoreAccessMode.DEBUG_EMULATOR_ANONYMOUS,
                firestoreEmulatorEnabled = true,
                isDebugBuild = true,
            )
        )

        runCurrent()

        assertEquals(FirebaseSessionPhase.READY, fixture.controller.state.value.phase)
        assertEquals(0, fixture.broker.exchangeCount)
    }

    @Test
    fun `logout continues when revoke fails`() = runTest {
        val fixture = fixture(revokeFailure = IllegalStateException("unavailable"))
        fixture.authRepository.authenticate(validTokens())
        runCurrent()

        fixture.controller.logout()

        assertEquals(FirebaseSessionPhase.SIGNED_OUT, fixture.controller.state.value.phase)
        assertEquals(1, fixture.broker.revokeCount)
        assertTrue(fixture.gateway.signOutCount > 0)
    }

    @Test
    fun `stored installation id remains stable`() {
        var stored: String? = null
        var generated = 0
        val provider = StoredInstallationIdProvider(
            store = object : InstallationIdStore {
                override fun read(): String? = stored
                override fun write(value: String) {
                    stored = value
                }
            },
            generate = {
                generated += 1
                "installation-$generated"
            },
        )

        assertEquals("installation-1", provider.installationId())
        assertEquals("installation-1", provider.installationId())
        assertEquals(1, generated)
    }

    private fun TestScope.fixture(
        config: AppRuntimeConfig = baseConfig(),
        gatewayUid: String = "firebase-user",
        signInFailures: Int = 0,
        grantExpiresAt: Long = 4_600,
        revokeFailure: Throwable? = null,
    ): Fixture {
        val session = AppSessionContext("tenant-a", "event-1", "fallback")
        val authRepository = AuthRepository(session)
        val broker = FakeBroker(grantExpiresAt, revokeFailure)
        val gateway = FakeGateway(gatewayUid, signInFailures)
        val controller = FirebaseSessionController(
            config = config,
            session = session,
            authRepository = authRepository,
            broker = broker,
            authGateway = gateway,
            installationIdProvider = InstallationIdProvider { "installation-1" },
            coroutineContext = StandardTestDispatcher(testScheduler),
            nowEpochSeconds = { 1_000 },
            randomJitterMillis = { 0 },
        )
        return Fixture(authRepository, broker, gateway, controller)
    }

    private fun baseConfig() = AppRuntimeConfig(
        tenantId = "tenant-a",
        eventId = "event-1",
        staffId = "fallback",
        grpcHost = "localhost",
        firestoreEnabled = true,
        firestoreProjectId = "project-a",
        firestoreAccessMode = FirestoreAccessMode.AUTHENTICATED,
        firebaseApplicationId = "app-a",
        firebaseApiKey = "api-key",
        firebaseGcmSenderId = "sender-a",
        oidcLoginEnabled = true,
        oidcIssuer = "https://issuer.example",
        oidcResourceBaseUrl = "https://api.example",
    )

    private data class Fixture(
        val authRepository: AuthRepository,
        val broker: FakeBroker,
        val gateway: FakeGateway,
        val controller: FirebaseSessionController,
    )

    private class FakeBroker(
        private val grantExpiresAt: Long,
        private val revokeFailure: Throwable?,
    ) : FirebaseSessionBroker {
        var exchangeCount = 0
        var revokeCount = 0
        lateinit var lastGrant: FirebaseSessionGrant

        override suspend fun exchange(installationId: String): FirebaseSessionGrant {
            exchangeCount += 1
            return FirebaseSessionGrant(
                customToken = "custom-secret",
                firebaseUid = "firebase-user",
                firebaseProjectId = "project-a",
                firestoreDatabaseId = null,
                grantVersion = 3,
                grantExpiresAtEpochSeconds = grantExpiresAt,
            ).also { lastGrant = it }
        }

        override suspend fun revoke(installationId: String) {
            revokeCount += 1
            revokeFailure?.let { throw it }
        }
    }

    private class FakeGateway(
        private val uid: String,
        private var remainingSignInFailures: Int,
    ) : FirebaseAuthGateway {
        var signOutCount = 0
        var signInCount = 0

        override suspend fun signInWithCustomToken(customToken: String): FirebaseSignInResult {
            signInCount += 1
            if (remainingSignInFailures > 0) {
                remainingSignInFailures -= 1
                error("sign-in failed")
            }
            return FirebaseSignInResult(uid)
        }

        override suspend fun currentUid(): String? = uid

        override suspend fun signOut() {
            signOutCount += 1
        }
    }
}
