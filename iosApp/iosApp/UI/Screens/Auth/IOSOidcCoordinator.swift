import AppAuth
import AuthenticationServices
import Foundation
import SharedLogic
import SwiftUI

@MainActor
final class IOSOidcCoordinator: NSObject, ASWebAuthenticationPresentationContextProviding {
    private let repository: AuthRepository
    private let config: AppRuntimeConfig
    private let vault = IOSKeychainAuthVault()
    private var authorizationFlow: OIDExternalUserAgentSession?
    private var logoutSession: ASWebAuthenticationSession?
    private var discoveredConfiguration: OIDServiceConfiguration?

    init(
        repository: AuthRepository = KoinHelper().getAuthRepository(),
        config: AppRuntimeConfig = KoinHelper().getRuntimeConfig()
    ) {
        self.repository = repository
        self.config = config
        super.init()
    }

    func restore() {
#if DEBUG
        if Self.isUITestAuthenticated {
            repository.authenticate(newTokens: Self.uiTestTokens)
            return
        }
        if Self.isUITestAuthFlow {
            repository.restore(restoredTokens: nil, nowEpochSeconds: Int64(Date.now.timeIntervalSince1970))
            return
        }
#endif
        let now = Int64(Date.now.timeIntervalSince1970)
        guard let tokens = vault.load() else {
            repository.restore(restoredTokens: nil, nowEpochSeconds: now)
            return
        }
        if tokens.eventExpiresAtEpochSeconds <= now + 60 {
            Task { await refresh(tokens) }
        } else {
            repository.restore(restoredTokens: tokens, nowEpochSeconds: now)
        }
    }

    func login() {
#if DEBUG
        if Self.isUITestAuthFlow {
            repository.startAuthorization()
            Task {
                try? await Task.sleep(for: .milliseconds(150))
                repository.startTokenExchange()
                try? await Task.sleep(for: .milliseconds(150))
                repository.authenticate(newTokens: Self.uiTestTokens)
            }
            return
        }
#endif
        repository.startAuthorization()
        guard let issuer = URL(string: config.oidcIssuer),
              let redirect = URL(string: config.oidcRedirectUri) else {
            repository.fail(failure: .configuration)
            return
        }
        OIDAuthorizationService.discoverConfiguration(forIssuer: issuer) { [weak self] configuration, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                guard let configuration, error == nil else {
                    repository.fail(failure: .configuration)
                    return
                }
                discoveredConfiguration = configuration
                let request = OIDAuthorizationRequest(
                    configuration: configuration,
                    clientId: config.oidcClientId,
                    clientSecret: nil,
                    scopes: config.oidcScopes.split(separator: " ").map(String.init),
                    redirectURL: redirect,
                    responseType: OIDResponseTypeCode,
                    additionalParameters: [
                        "audience": config.oidcAudience,
                        "resource": config.tenantResource(),
                    ]
                )
                guard let presenter = Self.presentingViewController() else {
                    repository.fail(failure: .configuration)
                    return
                }
                authorizationFlow = OIDAuthState.authState(byPresenting: request, presenting: presenter) { [weak self] state, error in
                    Task { @MainActor [weak self] in
                        await self?.completeAuthorization(state: state, error: error)
                    }
                }
            }
        }
    }

    func logout() {
        let idToken = repository.currentTokens()?.idToken
        vault.delete()
        repository.logout()
        guard let idToken else { return }
        Task {
            guard let configuration = try? await discoverIfNeeded() else { return }
            presentEndSession(configuration: configuration, idToken: idToken)
        }
    }

    private func presentEndSession(configuration: OIDServiceConfiguration, idToken: String) {
        guard configuration.endSessionEndpoint != nil,
              let redirect = URL(string: config.oidcRedirectUri) else { return }
        let request = OIDEndSessionRequest(
            configuration: configuration,
            idTokenHint: idToken,
            postLogoutRedirectURL: redirect,
            additionalParameters: nil
        )
        logoutSession = ASWebAuthenticationSession(
            url: request.endSessionRequestURL(),
            callbackURLScheme: redirect.scheme
        ) { [weak self] _, _ in
            self?.logoutSession = nil
        }
        logoutSession?.prefersEphemeralWebBrowserSession = false
        logoutSession?.presentationContextProvider = self
        logoutSession?.start()
    }

    private func completeAuthorization(state: OIDAuthState?, error: Error?) async {
        guard error == nil,
              let tokenResponse = state?.lastTokenResponse,
              let tenantToken = tokenResponse.accessToken else {
            let nsError = error as NSError?
            let wasCancelled = nsError?.domain == OIDGeneralErrorDomain
                && nsError?.code == OIDErrorCode.userCanceledAuthorizationFlow.rawValue
            repository.fail(failure: wasCancelled ? .cancelled : .authorization)
            return
        }
        repository.startTokenExchange()
        do {
            let event = try await exchangeEventToken(tenantToken: tenantToken, configuration: tokenResponse.request.configuration)
            guard let subject = Self.subject(from: tokenResponse.idToken ?? tenantToken) else {
                repository.fail(failure: .authorization)
                return
            }
            let now = Int64(Date.now.timeIntervalSince1970)
            let tokens = AuthTokens(
                tenantAccessToken: tenantToken,
                tenantExpiresAtEpochSeconds: Int64(tokenResponse.accessTokenExpirationDate?.timeIntervalSince1970 ?? Double(now + 900)),
                refreshToken: tokenResponse.refreshToken,
                idToken: tokenResponse.idToken,
                eventAccessToken: event.token,
                eventExpiresAtEpochSeconds: event.expiresAt,
                subject: subject
            )
            try vault.save(tokens)
            repository.authenticate(newTokens: tokens)
        } catch {
            repository.fail(failure: .tokenExchange)
        }
    }

    private func refresh(_ tokens: AuthTokens) async {
        guard let refreshToken = tokens.refreshToken else {
            vault.delete()
            repository.fail(failure: .sessionExpired)
            return
        }
        do {
            let configuration = try await discoverIfNeeded()
            let tenant = try await postToken(configuration.tokenEndpoint, parameters: [
                "grant_type": "refresh_token",
                "refresh_token": refreshToken,
                "client_id": config.oidcClientId,
            ])
            guard let tenantToken = tenant["access_token"] as? String else { throw OIDCError.invalidResponse }
            let event = try await exchangeEventToken(tenantToken: tenantToken, configuration: configuration)
            let now = Int64(Date.now.timeIntervalSince1970)
            let updated = AuthTokens(
                tenantAccessToken: tenantToken,
                tenantExpiresAtEpochSeconds: now + Int64(tenant["expires_in"] as? Int ?? 900),
                refreshToken: tenant["refresh_token"] as? String ?? refreshToken,
                idToken: tenant["id_token"] as? String ?? tokens.idToken,
                eventAccessToken: event.token,
                eventExpiresAtEpochSeconds: event.expiresAt,
                subject: tokens.subject
            )
            try vault.save(updated)
            repository.authenticate(newTokens: updated)
        } catch {
            vault.delete()
            repository.fail(failure: .sessionExpired)
        }
    }

    private func exchangeEventToken(tenantToken: String, configuration: OIDServiceConfiguration) async throws -> (token: String, expiresAt: Int64) {
        let response = try await postToken(configuration.tokenEndpoint, parameters: [
            "grant_type": "urn:ietf:params:oauth:grant-type:token-exchange",
            "subject_token": tenantToken,
            "subject_token_type": "urn:ietf:params:oauth:token-type:access_token",
            "client_id": config.oidcClientId,
            "audience": config.oidcAudience,
            "resource": config.eventResource(),
            "scope": config.oidcScopes.split(separator: " ").filter { $0.hasPrefix("events.") }.joined(separator: " "),
        ])
        guard let token = response["access_token"] as? String else { throw OIDCError.invalidResponse }
        let now = Int64(Date.now.timeIntervalSince1970)
        return (token, now + Int64(response["expires_in"] as? Int ?? 600))
    }

    private func postToken(_ endpoint: URL, parameters: [String: String]) async throws -> [String: Any] {
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = parameters.map { key, value in
            "\(key.urlFormEncoded)=\(value.urlFormEncoded)"
        }.sorted().joined(separator: "&").data(using: .utf8)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode,
              let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw OIDCError.invalidResponse
        }
        return object
    }

    private func discoverIfNeeded() async throws -> OIDServiceConfiguration {
        if let discoveredConfiguration { return discoveredConfiguration }
        guard let issuer = URL(string: config.oidcIssuer) else { throw OIDCError.invalidConfiguration }
        return try await withCheckedThrowingContinuation { continuation in
            OIDAuthorizationService.discoverConfiguration(forIssuer: issuer) { configuration, error in
                if let configuration { continuation.resume(returning: configuration) }
                else { continuation.resume(throwing: error ?? OIDCError.invalidConfiguration) }
            }
        }
    }

    private static func subject(from jwt: String) -> String? {
        let segments = jwt.split(separator: ".")
        guard segments.count > 1 else { return nil }
        var payload = String(segments[1]).replacing("-", with: "+").replacing("_", with: "/")
        payload += String(repeating: "=", count: (4 - payload.count % 4) % 4)
        guard let data = Data(base64Encoded: payload),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        return json["sub"] as? String
    }

    private static func presentingViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var controller = scene?.keyWindow?.rootViewController
        while let presented = controller?.presentedViewController { controller = presented }
        return controller
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        Self.presentingViewController()?.view.window ?? ASPresentationAnchor()
    }

    private enum OIDCError: Error { case invalidConfiguration, invalidResponse }

#if DEBUG
    private static var isUITestAuthenticated: Bool {
        ProcessInfo.processInfo.arguments.contains("--uitesting")
    }

    private static var isUITestAuthFlow: Bool {
        ProcessInfo.processInfo.arguments.contains("--uitesting-auth-flow")
    }

    private static var uiTestTokens: AuthTokens {
        let expiry = Int64(Date.now.timeIntervalSince1970) + 3_600
        return AuthTokens(
            tenantAccessToken: "ui-test-tenant",
            tenantExpiresAtEpochSeconds: expiry,
            refreshToken: nil,
            idToken: nil,
            eventAccessToken: "ui-test-event",
            eventExpiresAtEpochSeconds: expiry,
            subject: "tanaka"
        )
    }
#endif
}

private extension String {
    var urlFormEncoded: String {
        addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed.subtracting(CharacterSet(charactersIn: "+&="))) ?? self
    }
}
