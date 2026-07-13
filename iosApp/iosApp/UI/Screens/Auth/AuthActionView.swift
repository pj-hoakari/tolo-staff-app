import SharedLogic
import SwiftUI

struct AuthActionView: View {
    let phase: AuthPhase
    let onLogin: () -> Void

    var body: some View {
        switch phase {
        case .restoring, .authorizing, .exchanging:
            ProgressView()
                .accessibilityIdentifier("auth_progress")
        case .error:
            VStack {
                Text("auth_error")
                    .foregroundStyle(.red)
                    .accessibilityIdentifier("auth_error_message")
                Button("auth_retry", action: onLogin)
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier("auth_retry_button")
            }
        default:
            Button("auth_login", action: onLogin)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .accessibilityIdentifier("auth_login_button")
        }
    }
}
