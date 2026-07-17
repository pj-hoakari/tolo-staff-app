package dev.usbharu.tolo_staff.streaming

import dev.usbharu.tolo_staff.error.AppErrorReporter
import dev.usbharu.tolo_staff.runtime.AppSessionContext

class FirestoreOperationsStreamDataSource(
    grpcClient: GrpcCommunicationClient,
    session: AppSessionContext,
    changeNotifier: OperationsChangeNotifier,
    errorReporter: AppErrorReporter,
) : GrpcBackedOperationsStreamDataSource(
    remoteDataSource = GrpcOperationsPollingRemoteDataSource(grpcClient, session),
    changeNotifier = changeNotifier,
    errorReporter = errorReporter,
) {
    companion object {
        const val UNKNOWN_STAFF_ID = "unknown"
    }
}
