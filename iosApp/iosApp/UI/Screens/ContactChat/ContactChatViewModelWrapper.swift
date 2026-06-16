import Foundation
import SharedLogic

final class ContactChatViewModelWrapper: ObservableObject {
    @Published var state: ContactChatUiState

    private let viewModel: ContactChatViewModel
    private var stateJob: Kotlinx_coroutines_coreJob?

    init(viewModel: ContactChatViewModel = KoinHelper().getContactChatViewModel()) {
        self.viewModel = viewModel
        self.state = viewModel.uiState.value ?? ContactChatUiState(
            rooms: [],
            selectedRoomId: nil,
            selectedRoomTitle: nil,
            messages: [],
            draftText: "",
            isLoading: true,
            isRefreshing: false,
            isSending: false,
            errorMessage: nil
        )

        self.stateJob = viewModel.observeUiState { [weak self] state in
            guard let state else { return }
            DispatchQueue.main.async {
                self?.state = state
            }
        }
    }

    func onRoomSelected(_ roomId: String) {
        viewModel.onRoomSelected(roomId: roomId)
    }

    func onBackToRooms() {
        viewModel.onBackToRooms()
    }

    func onDraftChanged(_ text: String) {
        viewModel.onDraftChanged(text: text)
    }

    func onSendClicked() {
        viewModel.onSendClicked()
    }

    deinit {
        stateJob?.cancel(cause: nil)
        viewModel.clear()
    }
}
