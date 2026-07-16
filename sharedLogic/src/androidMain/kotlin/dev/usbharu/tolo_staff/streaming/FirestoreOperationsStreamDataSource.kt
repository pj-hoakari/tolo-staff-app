package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.runtime.AppSessionContext

class FirestoreOperationsStreamDataSource(
    grpcClient: GrpcCommunicationClient,
    session: AppSessionContext,
    changeNotifier: OperationsChangeNotifier,
) : GrpcBackedOperationsStreamDataSource(
    remoteDataSource = GrpcOperationsPollingRemoteDataSource(grpcClient, session),
    changeNotifier = changeNotifier,
) {
    companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
