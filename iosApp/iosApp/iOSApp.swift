import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        KoinInitializer.shared.start(config: Self.runtimeConfig)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private static var runtimeConfig: AppRuntimeConfig {
        let info = Bundle.main.infoDictionary ?? [:]
        let port = Int32((info["ToloGrpcPort"] as? String).flatMap(Int.init) ?? 8080)
        let firestorePort = Int32((info["ToloFirestorePort"] as? String).flatMap(Int.init) ?? 8081)
        let firestoreEnabled = (info["ToloFirestoreEnabled"] as? String)
            .map { ($0 as NSString).boolValue } ?? false
        let firestoreDatabaseId = (info["ToloFirestoreDatabaseId"] as? String)?.nilIfBlank
        return AppRuntimeConfig(
            tenantId: info["ToloTenantId"] as? String ?? "tenant-dev",
            eventId: info["ToloEventId"] as? String ?? "event-dev",
            staffId: info["ToloStaffId"] as? String ?? "tanaka",
            grpcHost: info["ToloGrpcHost"] as? String ?? "127.0.0.1",
            grpcPort: port,
            firestoreEnabled: firestoreEnabled,
            firestoreHost: info["ToloFirestoreHost"] as? String ?? "127.0.0.1",
            firestorePort: firestorePort,
            firestoreProjectId: info["ToloFirestoreProjectId"] as? String ?? "tolo-communication-poc",
            firestoreDatabaseId: firestoreDatabaseId
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self
    }
}
