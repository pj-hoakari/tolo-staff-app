import Observation
import SharedLogic

@MainActor
@Observable
final class AuthViewModelWrapper {
    var state: AuthUiState
    let coordinator: IOSOidcCoordinator

    private let viewModel: AuthViewModel
    private var stateJob: Kotlinx_coroutines_coreJob?

    init() {
        self.viewModel = KoinHelper().getAuthViewModel()
        self.coordinator = IOSOidcCoordinator()
        guard let initial = viewModel.uiState.value else {
            fatalError("AuthViewModel.uiState.value was unexpectedly nil")
        }
        state = initial
        observeState()
    }

    init(viewModel: AuthViewModel, coordinator: IOSOidcCoordinator) {
        self.viewModel = viewModel
        self.coordinator = coordinator
        guard let initial = viewModel.uiState.value else {
            fatalError("AuthViewModel.uiState.value was unexpectedly nil")
        }
        state = initial
        observeState()
    }

    private func observeState() {
        stateJob = viewModel.observeUiState { [weak self] state in
            guard let state else { return }
            Task { @MainActor [weak self] in self?.state = state }
        }
    }

    func restore() { coordinator.restore() }
    func login() { coordinator.login() }
    func logout() { coordinator.logout() }

    func clear() {
        stateJob?.cancel(cause: nil)
        viewModel.clear()
    }
}
