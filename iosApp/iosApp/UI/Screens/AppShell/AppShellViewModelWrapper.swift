import Foundation
import SharedLogic

final class AppShellViewModelWrapper: ObservableObject {
    @Published var state: AppShellUiState

    private let viewModel: AppShellViewModel
    private var stateJob: Kotlinx_coroutines_coreJob?

    init(viewModel: AppShellViewModel = KoinHelper().getAppShellViewModel()) {
        self.viewModel = viewModel
        self.state = viewModel.uiState.value ?? AppShellUiState.mock()

        self.stateJob = viewModel.observeUiState { [weak self] state in
            guard let state else { return }
            DispatchQueue.main.async {
                self?.state = state
            }
        }
    }

    func onTabSelected(_ tab: AppTab) {
        viewModel.onTabSelected(tab: tab)
    }

    deinit {
        stateJob?.cancel(cause: nil)
        viewModel.clear()
    }
}
