import SharedLogic
import SwiftUI

@MainActor
struct ContentView: View {
    @State private var auth: AuthViewModelWrapper?

    init(oidcLoginEnabled: Bool) {
        _auth = State(initialValue: oidcLoginEnabled ? AuthViewModelWrapper() : nil)
    }

    var body: some View {
        Group {
            if let auth {
                if auth.state.phase == .authenticated {
                    AppShellScreen(onLogout: auth.logout)
                } else {
                    AuthScreen(state: auth.state, onLogin: auth.login)
                }
            } else {
                AppShellScreen()
            }
        }
        .task { auth?.restore() }
        .onDisappear { auth?.clear() }
    }
}

#Preview {
    ContentView(oidcLoginEnabled: false)
}
