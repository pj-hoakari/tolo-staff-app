import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    private let oidcLoginEnabled: Bool

    init() {
        let config = Self.runtimeConfig
        oidcLoginEnabled = config.oidcLoginEnabled
        KoinInitializer.shared.start(config: config)
    }

    var body: some Scene {
        WindowGroup {
            ContentView(oidcLoginEnabled: oidcLoginEnabled)
        }
    }

    private static var runtimeConfig: AppRuntimeConfig {
        let info = Bundle.main.infoDictionary ?? [:]
        let port = Int32((info["ToloGrpcPort"] as? String).flatMap(Int.init) ?? 8080)
        let firestorePort = Int32((info["ToloFirestorePort"] as? String).flatMap(Int.init) ?? 8081)
        let firestoreEnabled = (info["ToloFirestoreEnabled"] as? String)
            .map { ($0 as NSString).boolValue } ?? false
        let firestoreDatabaseId = (info["ToloFirestoreDatabaseId"] as? String)?.nilIfBlank
        let configuredOidcLoginEnabled = (info["ToloOidcEnabled"] as? String)
            .map { ($0 as NSString).boolValue } ?? false
#if DEBUG
        let oidcLoginEnabled = configuredOidcLoginEnabled
            || ProcessInfo.processInfo.arguments.contains("--uitesting-auth-flow")
#else
        let oidcLoginEnabled = configuredOidcLoginEnabled
#endif
        return AppRuntimeConfig(
            tenantId: info["ToloTenantId"] as? String ?? "tenant-dev",
            eventId: info["ToloEventId"] as? String ?? "event-dev",
            staffId: oidcLoginEnabled ? "unauthenticated" : (info["ToloStaffId"] as? String ?? "tanaka"),
            grpcHost: info["ToloGrpcHost"] as? String ?? "127.0.0.1",
            grpcPort: port,
            firestoreEnabled: firestoreEnabled,
            firestoreHost: info["ToloFirestoreHost"] as? String ?? "127.0.0.1",
            firestorePort: firestorePort,
            firestoreProjectId: info["ToloFirestoreProjectId"] as? String ?? "tolo-communication-poc",
            firestoreDatabaseId: firestoreDatabaseId,
            oidcLoginEnabled: oidcLoginEnabled,
            oidcIssuer: info["ToloOidcIssuer"] as? String ?? "http://localhost:8080",
            oidcClientId: info["ToloOidcClientId"] as? String ?? "tolo-staff-mobile",
            oidcRedirectUri: info["ToloOidcRedirectUri"] as? String ?? "dev.usbharu.tolostaff:/oauth2redirect",
            oidcScopes: info["ToloOidcScopes"] as? String ?? "openid profile offline_access tenant.read tenant.write events.read events.write",
            oidcAudience: info["ToloOidcAudience"] as? String ?? "backend-api",
            oidcResourceBaseUrl: info["ToloOidcResourceBaseUrl"] as? String ?? "https://api.example.com"
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self
    }
}
