package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class FirestoreOperationsStreamDataSource(
    config: OperationsStreamingConfig = defaultOperationsStreamingConfig(),
    firestoreFactory: () -> FirebaseFirestore = {
        Firebase.firestore
    },
    grpcClient: GrpcCommunicationClient,
) : GrpcBackedOperationsStreamDataSource(
    remoteDataSource = GrpcOperationsPollingRemoteDataSource(grpcClient),
    changeNotifier = FirestoreOperationsChangeNotifier(
        config = config,
        firestoreFactory = firestoreFactory,
    ),
) {
    companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
