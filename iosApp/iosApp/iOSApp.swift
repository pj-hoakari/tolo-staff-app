import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    private let oidcLoginEnabled: Bool

    init() {
        let config = Self.runtimeConfig
        oidcLoginEnabled = config.oidcLoginEnabled
        KoinInitializer.shared.start(
            config: config,
            startupErrorScenario: Self.startupErrorScenario
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView(oidcLoginEnabled: oidcLoginEnabled)
        }
    }

    private static var startupErrorScenario: AppStartupErrorScenario {
#if DEBUG
        guard let value = ProcessInfo.processInfo.arguments
            .first(where: { $0.hasPrefix("--uitesting-error=") })?
            .split(separator: "=", maxSplits: 1)
            .last
        else {
            return AppStartupErrorScenario.none
        }

        switch value {
        case "grpc": return AppStartupErrorScenario.grpcConnection
        case "fatal": return AppStartupErrorScenario.fatal
        default: return AppStartupErrorScenario.none
        }
#else
        return AppStartupErrorScenario.none
#endif
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
        let isDebugBuild = true
        let oidcLoginEnabled = configuredOidcLoginEnabled
            || ProcessInfo.processInfo.arguments.contains("--uitesting-auth-flow")
#else
        let isDebugBuild = false
        let oidcLoginEnabled = configuredOidcLoginEnabled
#endif
        let tenantId: String = info["ToloTenantId"] as? String ?? "tenant-dev"
        let eventId: String = info["ToloEventId"] as? String ?? "event-dev"
        let staffId: String = oidcLoginEnabled
            ? "unauthenticated"
            : (info["ToloStaffId"] as? String ?? "tanaka")
        let grpcHost: String = info["ToloGrpcHost"] as? String ?? "127.0.0.1"
        let firestoreHost: String = info["ToloFirestoreHost"] as? String ?? "127.0.0.1"
        let firestoreProjectId: String = info["ToloFirestoreProjectId"] as? String ?? "tolo-communication-poc"
        let oidcIssuer: String = info["ToloOidcIssuer"] as? String ?? "http://localhost:8080"
        let oidcClientId: String = info["ToloOidcClientId"] as? String ?? "tolo-staff-mobile"
        let oidcRedirectUri: String = info["ToloOidcRedirectUri"] as? String ?? "dev.usbharu.tolostaff:/oauth2redirect"
        let oidcScopes: String = info["ToloOidcScopes"] as? String
            ?? "openid profile offline_access tenant.read tenant.write events.read events.write"
        let oidcAudience: String = info["ToloOidcAudience"] as? String ?? "backend-api"
        let oidcResourceBaseUrl: String = info["ToloOidcResourceBaseUrl"] as? String ?? "https://api.example.com"
        return AppRuntimeConfig(
            tenantId: tenantId,
            eventId: eventId,
            staffId: staffId,
            grpcHost: grpcHost,
            grpcPort: port,
            firestoreEnabled: firestoreEnabled,
            firestoreHost: firestoreHost,
            firestorePort: firestorePort,
            firestoreProjectId: firestoreProjectId,
            firestoreDatabaseId: firestoreDatabaseId,
            firestoreAccessMode: .disabled,
            firestoreEmulatorEnabled: false,
            firebaseAuthEmulatorEnabled: false,
            firebaseAuthHost: "127.0.0.1",
            firebaseAuthPort: 9099,
            firebaseApplicationId: "",
            firebaseApiKey: "",
            firebaseGcmSenderId: "",
            isDebugBuild: isDebugBuild,
            oidcLoginEnabled: oidcLoginEnabled,
            oidcIssuer: oidcIssuer,
            oidcClientId: oidcClientId,
            oidcRedirectUri: oidcRedirectUri,
            oidcScopes: oidcScopes,
            oidcAudience: oidcAudience,
            oidcResourceBaseUrl: oidcResourceBaseUrl
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self
    }
}
