package dev.usbharu.tolo_staff.streaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirebaseFailureClassificationTest {
    @Test
    fun `auth error codes use exact Firebase failure mapping`() {
        assertEquals(
            FirebaseSessionFailure.UNAVAILABLE,
            firebaseAuthFailureForCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
        assertEquals(
            FirebaseSessionFailure.RESOURCE_EXHAUSTED,
            firebaseAuthFailureForCode("ERROR_TOO_MANY_REQUESTS"),
        )
        assertNull(firebaseAuthFailureForCode("ERROR_INVALID_CUSTOM_TOKEN"))
        assertNull(firebaseAuthFailureForCode("ERROR_API_NOT_AVAILABLE"))
    }

    @Test
    fun `Firestore listener uses status code rather than exception message`() {
        assertEquals(
            FirestoreListenerFailureKind.TRANSIENT,
            firestoreListenerFailureKind(firestoreCodeValue = 14),
        )
        assertEquals(
            FirestoreListenerFailureKind.PERMISSION_DENIED,
            firestoreListenerFailureKind(firestoreCodeValue = 7),
        )
        assertEquals(
            FirestoreListenerFailureKind.FATAL,
            firestoreListenerFailureKind(firestoreCodeValue = 3),
        )
    }

    @Test
    fun `listener connection key is stable for the same document`() {
        assertEquals(
            listenerConnectionKey("operations_change_heads", "staff-1"),
            listenerConnectionKey("operations_change_heads", "staff-1"),
        )
    }

    @Test
    fun `permission recovery is attempted once across generations and reset by snapshot success`() {
        val tracker = FirestorePermissionRecoveryTracker()
        val connectionKey = listenerConnectionKey("operations_change_heads", "staff-1")

        assertEquals(true, tracker.shouldAttemptRecovery(connectionKey))
        assertEquals(false, tracker.shouldAttemptRecovery(connectionKey))

        tracker.recordSuccess(connectionKey)

        assertEquals(true, tracker.shouldAttemptRecovery(connectionKey))
    }
}
