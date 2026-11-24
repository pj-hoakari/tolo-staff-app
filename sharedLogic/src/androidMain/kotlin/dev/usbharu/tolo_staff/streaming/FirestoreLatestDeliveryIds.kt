package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.usbharu.tolo_staff.logging.AppLogger
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import tolo.realtime.v1.IssueFirestoreTokenRequest
import tolo.realtime.v1.RealtimeFetchService
import tolo.realtime.v1.invoke

class FirestoreLatestDeliveryIds(
    private val realtime: RealtimeFetchService,
    private val auth: () -> FirebaseAuth = { Firebase.auth },
    private val firestore: () -> FirebaseFirestore = { Firebase.firestore },
) {
    private val logger = AppLogger.withTag("FirestoreLatestDeliveryIds")

    fun observe(eventId: String): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            val token = realtime.IssueFirestoreToken(IssueFirestoreTokenRequest { this.eventId = eventId })
            auth().signInWithCustomToken(token.firebaseCustomToken)
            val validFor = Instant.fromEpochSeconds(token.expiresAt.seconds, token.expiresAt.nanos) - Clock.System.now()
            logger.info { "Signed in for delivery notifications: eventId=$eventId, validFor=$validFor" }
            withTimeoutOrNull(validFor) {
                firestore().collection(DELIVERY_NOTIFICATIONS_COLLECTION).document(eventId).snapshots
                    .mapNotNull { snapshot ->
                        if (snapshot.exists) snapshot.get<String?>(LATEST_DELIVERY_ID_FIELD) else null
                    }
                    .collect { emit(it) }
            }
        }
    }

    private companion object {
        const val DELIVERY_NOTIFICATIONS_COLLECTION = "deliveryNotifications"
        const val LATEST_DELIVERY_ID_FIELD = "latestDeliveryId"
    }
}
