package dev.usbharu.tolo_staff.streaming

import android.content.Context
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.app
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import dev.usbharu.tolo_staff.logging.AppLogger
import dev.usbharu.tolo_staff.runtime.AppRuntimeConfig
import dev.usbharu.tolo_staff.runtime.FirestoreAccessMode

internal object AndroidFirebasePlatformRuntime {
    lateinit var applicationContext: Context
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}

actual class OperationsFirebaseBootstrap {
    private val logger = AppLogger.withTag("OperationsFirebaseBootstrap")

    actual fun initialize(context: Any?, config: AppRuntimeConfig) {
        val androidContext = requireNotNull(context as? Context) {
            "Android Firebase initialization requires an Android Context"
        }
        AndroidFirebasePlatformRuntime.initialize(androidContext)

        if (Firebase.apps(androidContext).isEmpty()) {
            Firebase.initialize(
                context = androidContext,
                options = FirebaseOptions(
                    applicationId = config.firebaseApplicationId,
                    apiKey = config.firebaseApiKey,
                    projectId = config.firestoreProjectId,
                    gcmSenderId = config.firebaseGcmSenderId,
                )
            )
            logger.info { "Firebase app initialized: projectId=${config.firestoreProjectId}" }
        } else {
            logger.debug { "Using the existing Firebase app: projectId=${config.firestoreProjectId}" }
        }
        require(Firebase.app.options.projectId == config.firestoreProjectId) {
            "Firebase app project does not match firestoreProjectId"
        }

        synchronized(bootstrapLock) {
            if (config.firestoreEmulatorEnabled && !firestoreEmulatorConfigured) {
                Firebase.firestore(Firebase.app, config.firestoreDatabaseId)
                    .useEmulator(config.firestoreHost, config.firestorePort)
                firestoreEmulatorConfigured = true
                logger.info {
                    "Configured Firestore emulator: host=${config.firestoreHost}, port=${config.firestorePort}"
                }
            }
            if (
                config.firestoreAccessMode == FirestoreAccessMode.AUTHENTICATED &&
                config.firebaseAuthEmulatorEnabled &&
                !authEmulatorConfigured
            ) {
                Firebase.auth(Firebase.app).useEmulator(config.firebaseAuthHost, config.firebaseAuthPort)
                authEmulatorConfigured = true
                logger.info {
                    "Configured Firebase Auth emulator: host=${config.firebaseAuthHost}, port=${config.firebaseAuthPort}"
                }
            }
        }
    }

    private companion object {
        val bootstrapLock = Any()
        var firestoreEmulatorConfigured = false
        var authEmulatorConfigured = false
    }
}
