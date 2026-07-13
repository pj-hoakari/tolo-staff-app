import SnapshotTesting
import SwiftUI
import XCTest
import SharedLogic
@testable import ToloStaff

@MainActor
final class AuthViewTests: XCTestCase {
    func testAuthStatesMatchSnapshots() {
        let states: [(String, AuthUiState)] = [
            ("restoring", AuthUiState(phase: .restoring, subject: nil, failure: nil)),
            ("signed-out", AuthUiState(phase: .signedOut, subject: nil, failure: nil)),
            ("authorizing", AuthUiState(phase: .authorizing, subject: nil, failure: nil)),
            ("error", AuthUiState(phase: .error, subject: nil, failure: .network)),
        ]

        for (name, state) in states {
            let controller = UIHostingController(rootView: AuthContentView(state: state))
            controller.overrideUserInterfaceStyle = .light
            assertSnapshot(of: controller, as: .image(on: .iPhone13), named: name)
        }
    }

    func testWrapperSubscribesAndStopsAfterClear() async {
        let repository = KoinHelper().getAuthRepository()
        repository.logout()
        let wrapper = AuthViewModelWrapper()

        repository.startAuthorization()
        await waitUntil { wrapper.state.phase == .authorizing }
        XCTAssertEqual(wrapper.state.phase, .authorizing)

        wrapper.clear()
        repository.startTokenExchange()
        try? await Task.sleep(for: .milliseconds(100))
        XCTAssertEqual(wrapper.state.phase, .authorizing)
    }

    func testKeychainVaultRoundTripsAndDeletesTokens() throws {
        let vault = IOSKeychainAuthVault()
        let tokens = AuthTokens(
            tenantAccessToken: "tenant-secret",
            tenantExpiresAtEpochSeconds: 2_000_000_000,
            refreshToken: "refresh-secret",
            idToken: "id-secret",
            eventAccessToken: "event-secret",
            eventExpiresAtEpochSeconds: 2_000_000_000,
            subject: "staff-1"
        )

        vault.delete()
        try vault.save(tokens)
        let restored = try XCTUnwrap(vault.load())
        XCTAssertEqual(restored.tenantAccessToken, tokens.tenantAccessToken)
        XCTAssertEqual(restored.eventAccessToken, tokens.eventAccessToken)
        XCTAssertEqual(restored.subject, tokens.subject)
        vault.delete()
        XCTAssertNil(vault.load())
    }

    private func waitUntil(_ condition: @escaping @MainActor () -> Bool) async {
        for _ in 0..<20 where !condition() {
            try? await Task.sleep(for: .milliseconds(25))
        }
    }
}
