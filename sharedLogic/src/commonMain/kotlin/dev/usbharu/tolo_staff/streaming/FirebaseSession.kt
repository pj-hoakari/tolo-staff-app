package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo.communication.v1.ExchangeFirebaseCustomTokenRequest
import dev.usbharu.tolo.communication.v1.ExchangeFirebaseCustomTokenResponse
import dev.usbharu.tolo.communication.v1.FirebaseSessionService
import dev.usbharu.tolo.communication.v1.RevokeFirebaseSessionRequest
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.feature.auth.AuthPhase
import dev.usbharu.tolo_staff.feature.auth.AuthRefreshReason
import dev.usbharu.tolo_staff.feature.auth.AuthRepository
import dev.usbharu.tolo_staff.error.AppErrorReporter
import dev.usbharu.tolo_staff.error.AppErrorSource
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import dev.usbharu.tolo_staff.runtime.FirestoreAccessMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.rpc.grpc.GrpcStatusCode
import kotlinx.rpc.grpc.GrpcStatusException
import kotlinx.rpc.grpc.status
import kotlinx.rpc.grpc.statusCode
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Clock

enum class FirebaseSessionPhase {
    DISABLED,
    SIGNED_OUT,
    EXCHANGING,
    SIGNING_IN,
    READY,
    REFRESHING,
    ERROR,
}

enum class FirebaseSessionFailure {
    UNAUTHENTICATED,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    UNAVAILABLE,
    CONFIGURATION,
    SIGN_IN,
    UNKNOWN,
}

internal class FirebaseSessionOperationException(
    val failure: FirebaseSessionFailure,
    cause: Throwable,
) : Exception("Firebase operation failed: $failure", cause)

data class FirebaseSessionState(
    val phase: FirebaseSessionPhase,
    val generation: Long = 0,
    val tenantId: String? = null,
    val eventId: String? = null,
    val staffId: String? = null,
    val firebaseUid: String? = null,
    val grantVersion: Long = 0,
    val grantExpiresAtEpochSeconds: Long? = null,
    val failure: FirebaseSessionFailure? = null,
    val retryCount: Int = 0,
) {
    val isReady: Boolean get() = phase == FirebaseSessionPhase.READY
}

data class FirebaseSignInResult(val uid: String)

interface FirebaseAuthGateway {
    suspend fun signInWithCustomToken(customToken: String): FirebaseSignInResult

    suspend fun currentUid(): String?

    suspend fun signOut()
}

object DisabledFirebaseAuthGateway : FirebaseAuthGateway {
    override suspend fun signInWithCustomToken(customToken: String): FirebaseSignInResult =
        error("Firebase Authentication is disabled")

    override suspend fun currentUid(): String? = null

    override suspend fun signOut() = Unit
}

fun interface InstallationIdProvider {
    fun installationId(): String
}

interface InstallationIdStore {
    fun read(): String?

    fun write(value: String)
}

class StoredInstallationIdProvider(
    private val store: InstallationIdStore,
    private val generate: () -> String,
) : InstallationIdProvider {
    override fun installationId(): String {
        store.read()?.takeIf(String::isNotBlank)?.let { return it }
        return generate().also(store::write)
    }
}

data class FirebaseSessionGrant(
    val customToken: String,
    val firebaseUid: String,
    val firebaseProjectId: String,
    val firestoreDatabaseId: String?,
    val grantVersion: Long,
    val grantExpiresAtEpochSeconds: Long,
) {
    override fun toString(): String =
        "FirebaseSessionGrant(firebaseUid=$firebaseUid, firebaseProjectId=$firebaseProjectId, " +
            "firestoreDatabaseId=$firestoreDatabaseId, grantVersion=$grantVersion, " +
            "grantExpiresAtEpochSeconds=$grantExpiresAtEpochSeconds, customToken=[REDACTED])"
}

interface FirebaseSessionBroker {
    suspend fun exchange(installationId: String): FirebaseSessionGrant

    suspend fun revoke(installationId: String)
}

class GrpcFirebaseSessionBroker(
    private val service: FirebaseSessionService,
    private val session: AppSessionContext,
) : FirebaseSessionBroker {
    override suspend fun exchange(installationId: String): FirebaseSessionGrant =
        service.ExchangeFirebaseCustomToken(
            ExchangeFirebaseCustomTokenRequest {
                scope = session.toProtoScope()
                this.installationId = installationId
            }
        ).toGrant()

    override suspend fun revoke(installationId: String) {
        service.RevokeFirebaseSession(
            RevokeFirebaseSessionRequest {
                scope = session.toProtoScope()
                this.installationId = installationId
            }
        )
    }
}

private fun ExchangeFirebaseCustomTokenResponse.toGrant(): FirebaseSessionGrant = FirebaseSessionGrant(
    customToken = customToken,
    firebaseUid = firebaseUid,
    firebaseProjectId = firebaseProjectId,
    firestoreDatabaseId = firestoreDatabaseId.ifBlank { null },
    grantVersion = grantVersion,
    grantExpiresAtEpochSeconds = grantExpiresAt.seconds,
)

class FirebaseSessionController(
    private val config: AppRuntimeConfig,
    private val session: AppSessionContext,
    private val authRepository: AuthRepository,
    private val broker: FirebaseSessionBroker,
    private val authGateway: FirebaseAuthGateway,
    private val installationIdProvider: InstallationIdProvider,
    private val errorReporter: AppErrorReporter = AppErrorReporter(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
    private val nowEpochSeconds: () -> Long = { Clock.System.now().epochSeconds },
    private val randomJitterMillis: (Long) -> Long = { upper -> if (upper <= 0) 0 else Random.nextLong(upper) },
) {
    private val logger = AppLogger.withTag("FirebaseSessionController")
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(initialState(config.firestoreAccessMode))
    private val isForeground = MutableStateFlow(true)
    private var generation = 0L
    private var establishedRevision = -1L
    private var establishedStaffId: String? = null
    private var refreshJob: Job? = null
    private var permissionRecoveryGeneration: Long? = null
    private var refreshPendingInBackground = false

    val state: StateFlow<FirebaseSessionState> = _state.asStateFlow()

    init {
        combine(
            authRepository.state,
            authRepository.revision,
            session.staffIdFlow,
        ) { authState, revision, staffId -> Triple(authState.phase, revision, staffId) }
            .distinctUntilChanged()
            .onEach { (phase, revision, staffId) ->
                when (config.firestoreAccessMode) {
                    FirestoreAccessMode.DISABLED -> transition(FirebaseSessionState(FirebaseSessionPhase.DISABLED))
                    FirestoreAccessMode.DEBUG_EMULATOR_ANONYMOUS -> establishAnonymousDebug(staffId)
                    FirestoreAccessMode.AUTHENTICATED -> {
                        if (phase == AuthPhase.AUTHENTICATED) {
                            if (revision != establishedRevision || staffId != establishedStaffId) {
                                establishAuthenticated(revision, staffId)
                            }
                        } else {
                            leaveReady(FirebaseSessionPhase.SIGNED_OUT)
                            runCatching { authGateway.signOut() }
                                .onFailure { logger.warn(it) { "Firebase sign-out after OIDC sign-out failed" } }
                        }
                    }
                }
            }
            .launchIn(scope)

        isForeground
            .onEach { foreground ->
                if (foreground && refreshPendingInBackground) {
                    refreshPendingInBackground = false
                    requestOidcRefresh(AuthRefreshReason.FOREGROUND_RESUME)
                }
            }
            .launchIn(scope)
    }

    fun onForeground() {
        logger.debug { "Application entered foreground" }
        isForeground.value = true
    }

    fun onBackground() {
        logger.debug { "Application entered background" }
        isForeground.value = false
    }

    suspend fun recoverFromPermissionDenied(): Boolean = mutex.withLock {
        if (config.firestoreAccessMode != FirestoreAccessMode.AUTHENTICATED) return@withLock false
        val current = state.value
        if (permissionRecoveryGeneration == current.generation) return@withLock true
        if (!current.isReady || permissionRecoveryGeneration != null) return@withLock false
        permissionRecoveryGeneration = current.generation
        logger.warn { "Firestore permission denied; requesting one Firebase session recovery: generation=${current.generation}" }
        leaveReady(FirebaseSessionPhase.REFRESHING)
        if (!authRepository.requestRefresh(AuthRefreshReason.FIRESTORE_PERMISSION_DENIED)) {
            logger.warn { "Firestore permission recovery request could not be queued" }
            transition(
                state.value.copy(
                    phase = FirebaseSessionPhase.ERROR,
                    failure = FirebaseSessionFailure.UNAUTHENTICATED,
                    retryCount = 0,
                )
            )
            permissionRecoveryGeneration = null
            return@withLock false
        }
        true
    }

    suspend fun logout() {
        logger.info { "Firebase logout started" }
        mutex.withLock {
            refreshJob?.cancel()
            leaveReady(FirebaseSessionPhase.SIGNED_OUT)
            if (config.firestoreAccessMode == FirestoreAccessMode.AUTHENTICATED) {
                runCatching { broker.revoke(installationIdProvider.installationId()) }
                    .onFailure { logger.warn(it) { "Firebase session revoke failed; continuing local logout" } }
                runCatching { authGateway.signOut() }
                    .onFailure { logger.warn(it) { "Firebase sign-out failed; continuing local logout" } }
            }
            establishedRevision = -1
            establishedStaffId = null
        }
        logger.info { "Firebase logout completed" }
    }

    private fun establishAnonymousDebug(staffId: String) {
        if (state.value.isReady && state.value.staffId == staffId) return
        generation += 1
        transition(
            FirebaseSessionState(
                phase = FirebaseSessionPhase.READY,
                generation = generation,
                tenantId = session.tenantId,
                eventId = session.eventId,
                staffId = staffId,
            )
        )
    }

    private fun establishAuthenticated(revision: Long, staffId: String) {
        scope.launch {
            mutex.withLock {
                val tokens = authRepository.currentTokens() ?: return@withLock
                val scopeTenantId = session.tenantId
                val scopeEventId = session.eventId
                transition(
                    state.value.copy(
                        phase = if (state.value.generation == 0L) FirebaseSessionPhase.EXCHANGING else FirebaseSessionPhase.REFRESHING,
                        failure = null,
                        retryCount = 0,
                    )
                )

                var authenticatedGrant: FirebaseSessionGrant? = null
                for (signInAttempt in 1..MAX_SIGN_IN_ATTEMPTS) {
                    transition(state.value.copy(phase = FirebaseSessionPhase.EXCHANGING))
                    val grant = exchangeWithRetry() ?: return@withLock
                    if (
                        session.tenantId != scopeTenantId ||
                        session.eventId != scopeEventId ||
                        session.staffId != staffId ||
                        authRepository.currentTokens() !== tokens
                    ) {
                        logger.warn { "Firebase exchange result discarded because the application session changed" }
                        return@withLock
                    }
                    if (
                        grant.firebaseProjectId != config.firestoreProjectId ||
                        normalizeDatabaseId(grant.firestoreDatabaseId) != normalizeDatabaseId(config.firestoreDatabaseId) ||
                        grant.grantExpiresAtEpochSeconds <= nowEpochSeconds()
                    ) {
                        runCatching { authGateway.signOut() }
                        fail(FirebaseSessionFailure.CONFIGURATION, 0)
                        return@withLock
                    }

                    transition(state.value.copy(phase = FirebaseSessionPhase.SIGNING_IN))
                    val signInResult = try {
                        authGateway.signInWithCustomToken(grant.customToken)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        val failure = classifyFailure(throwable).let { classified ->
                            if (classified == FirebaseSessionFailure.UNKNOWN) FirebaseSessionFailure.SIGN_IN else classified
                        }
                        logger.warn(throwable) {
                            "Firebase Custom Token sign-in failed: attempt=$signInAttempt, " +
                                "failure=$failure, exception=${throwable::class.simpleName}"
                        }
                        if (failure == FirebaseSessionFailure.UNAVAILABLE ||
                            failure == FirebaseSessionFailure.RESOURCE_EXHAUSTED
                        ) {
                            errorReporter.reportConnectionFailure(
                                source = AppErrorSource.FIREBASE,
                                throwable = throwable,
                                operation = "Firebase Custom Token sign-in: attempt=$signInAttempt",
                                connectionKey = FIREBASE_SESSION_CONNECTION_KEY,
                            )
                        }
                        if (signInAttempt == MAX_SIGN_IN_ATTEMPTS) {
                            fail(failure, signInAttempt - 1)
                            return@withLock
                        }
                        continue
                    }
                    val currentUid = authGateway.currentUid()
                    if (signInResult.uid != grant.firebaseUid || currentUid != grant.firebaseUid) {
                        runCatching { authGateway.signOut() }
                        fail(FirebaseSessionFailure.CONFIGURATION, 0)
                        return@withLock
                    }
                    authenticatedGrant = grant
                    break
                }
                val grant = authenticatedGrant ?: return@withLock

                generation += 1
                establishedRevision = revision
                establishedStaffId = staffId
                permissionRecoveryGeneration = null
                transition(
                    FirebaseSessionState(
                        phase = FirebaseSessionPhase.READY,
                        generation = generation,
                        tenantId = scopeTenantId,
                        eventId = scopeEventId,
                        staffId = staffId,
                        firebaseUid = grant.firebaseUid,
                        grantVersion = grant.grantVersion,
                        grantExpiresAtEpochSeconds = grant.grantExpiresAtEpochSeconds,
                    )
                )
                errorReporter.reportBackendRecovered(AppErrorSource.FIREBASE, FIREBASE_SESSION_CONNECTION_KEY)
                scheduleRefresh(grant.grantExpiresAtEpochSeconds)
            }
        }
    }

    private suspend fun exchangeWithRetry(): FirebaseSessionGrant? {
        var attempt = 0
        while (attempt < MAX_EXCHANGE_ATTEMPTS) {
            attempt += 1
            try {
                logger.debug { "Firebase Custom Token exchange started: attempt=$attempt" }
                return broker.exchange(installationIdProvider.installationId())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                val failure = classifyFailure(throwable)
                val retryable = failure == FirebaseSessionFailure.UNAVAILABLE ||
                    failure == FirebaseSessionFailure.RESOURCE_EXHAUSTED
                if (!retryable || attempt >= MAX_EXCHANGE_ATTEMPTS) {
                    logger.warn(throwable) { "Firebase Custom Token exchange failed: attempt=$attempt, failure=$failure" }
                    fail(failure, attempt - 1)
                    return null
                }
                val backoff = min(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS shl (attempt - 1))
                val delayMillis = backoff + randomJitterMillis(backoff / 2)
                logger.debug { "Retrying Firebase Custom Token exchange: attempt=$attempt, delayMillis=$delayMillis" }
                delay(delayMillis)
            }
        }
        return null
    }

    private fun scheduleRefresh(expiresAtEpochSeconds: Long) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val refreshAt = expiresAtEpochSeconds - REFRESH_LEEWAY_SECONDS
            val delayMillis = ((refreshAt - nowEpochSeconds()).coerceAtLeast(0)) * 1_000
            delay(delayMillis)
            leaveReady(FirebaseSessionPhase.REFRESHING, cancelRefreshJob = false)
            if (isForeground.value) {
                requestOidcRefresh(AuthRefreshReason.FIREBASE_GRANT_EXPIRING)
            } else {
                refreshPendingInBackground = true
                logger.info { "Firebase refresh deferred until foreground" }
            }
        }
    }

    private fun requestOidcRefresh(reason: AuthRefreshReason) {
        if (!authRepository.requestRefresh(reason)) {
            logger.warn { "OIDC refresh request could not be queued: reason=$reason" }
            fail(FirebaseSessionFailure.UNAUTHENTICATED, 0)
        }
    }

    private fun leaveReady(
        phase: FirebaseSessionPhase,
        cancelRefreshJob: Boolean = true,
    ) {
        if (cancelRefreshJob) refreshJob?.cancel()
        transition(
            if (phase == FirebaseSessionPhase.SIGNED_OUT || phase == FirebaseSessionPhase.DISABLED) {
                FirebaseSessionState(phase = phase, generation = state.value.generation)
            } else {
                state.value.copy(phase = phase, failure = null, retryCount = 0)
            }
        )
    }

    private fun fail(failure: FirebaseSessionFailure, retryCount: Int) {
        refreshJob?.cancel()
        val operation = "Establish Firebase session: failure=$failure, retryCount=$retryCount"
        if (failure == FirebaseSessionFailure.UNAVAILABLE || failure == FirebaseSessionFailure.RESOURCE_EXHAUSTED) {
            errorReporter.reportConnectionFailure(
                source = AppErrorSource.FIREBASE,
                operation = operation,
                connectionKey = FIREBASE_SESSION_CONNECTION_KEY,
            )
        } else {
            errorReporter.reportBackendFailure(
                AppErrorSource.FIREBASE,
                IllegalStateException("Firebase session failure: $failure"),
                operation,
                FIREBASE_SESSION_CONNECTION_KEY,
            )
        }
        transition(state.value.copy(phase = FirebaseSessionPhase.ERROR, failure = failure, retryCount = retryCount))
    }

    private fun transition(next: FirebaseSessionState) {
        val previous = _state.value
        if (previous == next) return
        _state.value = next
        logger.info {
            "Firebase session state changed: from=${previous.phase}, to=${next.phase}, generation=${next.generation}, " +
                "tenantId=${next.tenantId}, eventId=${next.eventId}, staffId=${next.staffId}, " +
                "grantVersion=${next.grantVersion}, failure=${next.failure}, retryCount=${next.retryCount}"
        }
    }

    private fun classifyFailure(throwable: Throwable): FirebaseSessionFailure {
        val failures = generateSequence<Throwable>(throwable) { it.cause }
        failures.filterIsInstance<FirebaseSessionOperationException>()
            .firstOrNull()
            ?.let { return it.failure }
        val grpcStatus = generateSequence<Throwable>(throwable) { it.cause }
            .filterIsInstance<GrpcStatusException>()
            .firstOrNull()
            ?.status
            ?.statusCode
        return when (grpcStatus) {
            GrpcStatusCode.UNAUTHENTICATED -> FirebaseSessionFailure.UNAUTHENTICATED
            GrpcStatusCode.PERMISSION_DENIED -> FirebaseSessionFailure.PERMISSION_DENIED
            GrpcStatusCode.RESOURCE_EXHAUSTED -> FirebaseSessionFailure.RESOURCE_EXHAUSTED
            GrpcStatusCode.UNAVAILABLE, GrpcStatusCode.DEADLINE_EXCEEDED -> FirebaseSessionFailure.UNAVAILABLE
            else -> FirebaseSessionFailure.UNKNOWN
        }
    }

    private companion object {
        const val REFRESH_LEEWAY_SECONDS = 60L
        const val MAX_EXCHANGE_ATTEMPTS = 3
        const val MAX_SIGN_IN_ATTEMPTS = 2
        const val BASE_BACKOFF_MILLIS = 500L
        const val MAX_BACKOFF_MILLIS = 5_000L
        const val FIREBASE_SESSION_CONNECTION_KEY = "firebase.session"
    }
}

private fun initialState(mode: FirestoreAccessMode): FirebaseSessionState =
    FirebaseSessionState(
        phase = if (mode == FirestoreAccessMode.DISABLED) {
            FirebaseSessionPhase.DISABLED
        } else {
            FirebaseSessionPhase.SIGNED_OUT
        }
    )

private fun normalizeDatabaseId(value: String?): String = value.orEmpty().ifBlank { "(default)" }
