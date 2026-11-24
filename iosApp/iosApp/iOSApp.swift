import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        let environment = ProcessInfo.processInfo.environment
        KoinInitializer.shared.start(
            scope: OperationsScope(
                eventId: environment["TOLO_EVENT_ID"] ?? "",
                tenantId: environment["TOLO_TENANT_ID"] ?? "",
                accessToken: environment["TOLO_ACCESS_TOKEN"] ?? ""
            )
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
