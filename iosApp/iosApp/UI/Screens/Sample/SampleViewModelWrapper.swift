import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class SampleViewModelWrapper {
    var state: SampleUiState

    private let viewModel: SampleViewModel
    private var stateJob: Kotlinx_coroutines_coreJob?
    private var effectJob: Kotlinx_coroutines_coreJob?

    init(viewModel: SampleViewModel = KoinHelper().getSampleViewModel()) {
        self.viewModel = viewModel
        self.state = viewModel.uiState.value ?? SampleUiState(
            message: "KMP is ready",
            tapCount: 0,
            lastAction: nil
        )

        self.stateJob = viewModel.observeUiState { [weak self] state in
            guard let state else { return }
            Task { @MainActor [weak self] in
                self?.state = state
            }
        }

        self.effectJob = viewModel.observeEffect { effect in
            guard effect != nil else { return }
        }
    }

    func onPrimaryActionClicked() {
        viewModel.onPrimaryActionClicked()
    }

    func clear() {
        stateJob?.cancel(cause: nil)
        effectJob?.cancel(cause: nil)
        viewModel.clear()
    }
}
