import SharedLogic
import SwiftUI

@MainActor
struct AuthScreen: View {
    let state: AuthUiState
    var onLogin: () -> Void = {}

    var body: some View {
        AuthContentView(state: state, onLogin: onLogin)
    }
}
