import SharedLogic
import SwiftUI

@MainActor
struct ContentView: View {
    @State private var auth = AuthViewModelWrapper()

    var body: some View {
        Group {
            if auth.state.phase == .authenticated {
                AppShellScreen(onLogout: auth.logout)
            } else {
                AuthScreen(state: auth.state, onLogin: auth.login)
            }
        }
        .task { auth.restore() }
        .onDisappear { auth.clear() }
    }
}

#Preview {
    AuthContentView(state: AuthUiState(phase: .signedOut, subject: nil, failure: nil))
}
