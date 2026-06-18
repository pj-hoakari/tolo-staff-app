package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize

actual class OperationsFirebaseBootstrap {
    actual fun initialize(context: Any?) {
        if (Firebase.apps(context).isNotEmpty()) {
            return
        }

        Firebase.initialize(
            context = context,
            options = FirebaseOptions(
                applicationId = "1:1234567890:tolostaff:streaming",
                apiKey = "fake-api-key",
                projectId = "tolo-communication-poc",
                gcmSenderId = "1234567890",
            )
        )
        Firebase.firestore.useEmulator("10.0.2.2", 8081)
    }
}
