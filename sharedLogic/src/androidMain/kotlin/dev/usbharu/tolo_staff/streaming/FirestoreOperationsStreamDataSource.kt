package dev.usbharu.tolo_staff.streaming

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.usbharu.tolo_staff.runtime.AppSessionContext

class FirestoreOperationsStreamDataSource(
    config: OperationsStreamingConfig = defaultOperationsStreamingConfig(),
    firestoreFactory: () -> FirebaseFirestore = {
        Firebase.firestore(Firebase.app, config.databaseId)
    },
    grpcClient: GrpcCommunicationClient,
    session: AppSessionContext,
) : GrpcBackedOperationsStreamDataSource(
    remoteDataSource = GrpcOperationsPollingRemoteDataSource(grpcClient, session),
    changeNotifier = FirestoreOperationsChangeNotifier(
        config = config,
        session = session,
        firestoreFactory = firestoreFactory,
    ),
) {
    companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
