import SharedLogic
import SwiftUI

struct AuthContentView: View {
    let state: AuthUiState
    var onLogin: () -> Void = {}

    var body: some View {
        VStack {
            Spacer()
            Image(systemName: "person.badge.key.fill")
                .font(.largeTitle)
                .foregroundStyle(.tint)
                .accessibilityHidden(true)
            Text("auth_title")
                .font(.largeTitle)
                .bold()
            Text("auth_description")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            AuthActionView(phase: state.phase, onLogin: onLogin)
            Spacer()
        }
        .padding()
    }
}

#Preview {
    AuthContentView(state: AuthUiState(phase: .signedOut, subject: nil, failure: nil))
}
