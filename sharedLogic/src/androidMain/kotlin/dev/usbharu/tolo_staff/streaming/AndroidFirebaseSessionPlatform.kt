package dev.usbharu.tolo_staff.streaming

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import java.util.UUID

class AndroidFirebaseAuthGateway(
    private val auth: FirebaseAuth = Firebase.auth(Firebase.app),
) : FirebaseAuthGateway {
    override suspend fun signInWithCustomToken(customToken: String): FirebaseSignInResult {
        val result = try {
            auth.signInWithCustomToken(customToken)
        } catch (throwable: Throwable) {
            val failure = throwable.firebaseSessionFailureOrNull() ?: throw throwable
            throw FirebaseSessionOperationException(failure, throwable)
        }
        val uid = result.user?.uid ?: error("Firebase Custom Token sign-in returned no user")
        return FirebaseSignInResult(uid)
    }

    override suspend fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun signOut() {
        auth.signOut()
    }
}

internal fun Throwable.firebaseSessionFailureOrNull(): FirebaseSessionFailure? =
    generateSequence<Throwable>(this) { it.cause }
        .mapNotNull { cause ->
            when (cause) {
                is FirebaseNetworkException -> FirebaseSessionFailure.UNAVAILABLE

                is FirebaseTooManyRequestsException -> FirebaseSessionFailure.RESOURCE_EXHAUSTED
                is FirebaseAuthException -> firebaseAuthFailureForCode(cause.errorCode)

                else -> null
            }
        }
        .firstOrNull()

internal fun firebaseAuthFailureForCode(errorCode: String): FirebaseSessionFailure? =
    when (errorCode) {
        "ERROR_NETWORK_REQUEST_FAILED" -> FirebaseSessionFailure.UNAVAILABLE
        "ERROR_TOO_MANY_REQUESTS" -> FirebaseSessionFailure.RESOURCE_EXHAUSTED
        else -> null
    }

class AndroidInstallationIdProvider(
    context: Context = AndroidFirebasePlatformRuntime.applicationContext,
) : InstallationIdProvider {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val delegate = StoredInstallationIdProvider(
        store = object : InstallationIdStore {
            override fun read(): String? = preferences.getString(KEY_INSTALLATION_ID, null)

            override fun write(value: String) {
                check(preferences.edit().putString(KEY_INSTALLATION_ID, value).commit()) {
                    "Failed to persist Firebase installation ID"
                }
            }
        },
        generate = { UUID.randomUUID().toString() },
    )

    override fun installationId(): String = delegate.installationId()

    private companion object {
        const val PREFERENCES_NAME = "tolo_installation"
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}
