import Foundation
import Security
import SharedLogic

struct IOSKeychainAuthVault {
    private let service = "dev.usbharu.tolostaff.oidc"
    private let account = "tokens"

    func save(_ tokens: AuthTokens) throws {
        var object: [String: Any] = [
            "tenantAccessToken": tokens.tenantAccessToken,
            "tenantExpiresAt": tokens.tenantExpiresAtEpochSeconds,
            "eventAccessToken": tokens.eventAccessToken,
            "eventExpiresAt": tokens.eventExpiresAtEpochSeconds,
            "subject": tokens.subject,
        ]
        if let refreshToken = tokens.refreshToken, !refreshToken.isEmpty {
            object["refreshToken"] = refreshToken
        }
        if let idToken = tokens.idToken, !idToken.isEmpty {
            object["idToken"] = idToken
        }
        let data = try JSONSerialization.data(withJSONObject: object)
        delete()
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: data,
        ]
        guard SecItemAdd(query as CFDictionary, nil) == errSecSuccess else {
            throw VaultError.writeFailed
        }
    }

    func load() -> AuthTokens? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data,
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let tenantToken = json["tenantAccessToken"] as? String,
              let tenantExpiry = (json["tenantExpiresAt"] as? NSNumber)?.int64Value,
              let eventToken = json["eventAccessToken"] as? String,
              let eventExpiry = (json["eventExpiresAt"] as? NSNumber)?.int64Value,
              let subject = json["subject"] as? String else { return nil }
        return AuthTokens(
            tenantAccessToken: tenantToken,
            tenantExpiresAtEpochSeconds: tenantExpiry,
            refreshToken: json["refreshToken"] as? String,
            idToken: json["idToken"] as? String,
            eventAccessToken: eventToken,
            eventExpiresAtEpochSeconds: eventExpiry,
            subject: subject
        )
    }

    func delete() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
    }

    private enum VaultError: Error { case writeFailed }
}
