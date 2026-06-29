package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import dev.usbharu.tolo_staff.logging.AppLogger

actual class OperationsFirebaseBootstrap {
    private val logger = AppLogger.withTag("OperationsFirebaseBootstrap")

    actual fun initialize(context: Any?) {
        if (Firebase.apps(context).isNotEmpty()) {
            logger.debug { "Firebase initialization skipped because an app already exists" }
            return
        }

        val config = OperationsFirebaseRuntime.streamingConfig
        Firebase.initialize(
            context = context,
            options = FirebaseOptions(
                applicationId = "1:1234567890:tolostaff:streaming",
                apiKey = "fake-api-key",
                projectId = "tolo-communication-poc",
                gcmSenderId = "1234567890",
            )
        )
        if (config.enabled) {
            Firebase.firestore.useEmulator(config.host, config.port)
            logger.info {
                "Configured Firestore emulator during bootstrap: host=${config.host}, port=${config.port}"
            }
        } else {
            logger.info { "Firestore emulator bootstrap skipped because streaming is disabled" }
        }
    }
}
