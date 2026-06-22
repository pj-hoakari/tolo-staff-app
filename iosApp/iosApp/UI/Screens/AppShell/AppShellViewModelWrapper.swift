import Foundation
import SharedLogic

final class AppShellViewModelWrapper: ObservableObject {
    @Published var state: AppShellUiState

    private let viewModel: AppShellViewModel
    private var stateJob: Kotlinx_coroutines_coreJob?

    init(viewModel: AppShellViewModel = KoinHelper().getAppShellViewModel()) {
        self.viewModel = viewModel
        guard let initialState = viewModel.uiState.value else {
            fatalError("AppShellViewModel.uiState.value was unexpectedly nil")
        }
        self.state = initialState

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

    func onHomeInstructionSelected() {
        viewModel.onHomeInstructionSelected()
    }

    func onPlacementChangeConfirmed() {
        viewModel.onPlacementChangeConfirmed()
    }

    func onPlacementArrivalConfirmed() {
        viewModel.onPlacementArrivalConfirmed()
    }

    func onInstructionSelected(_ instructionId: String) {
        viewModel.onInstructionSelected(instructionId: instructionId)
    }

    func onInstructionThreadOpened() {
        viewModel.onInstructionThreadOpened()
    }

    func onInstructionDetailClosed() {
        viewModel.onInstructionDetailClosed()
    }

    func onReportTypeSelected(_ typeId: String) {
        viewModel.onReportTypeSelected(typeId: typeId)
    }

    func onReportSelected(_ reportId: String) {
        viewModel.onReportSelected(reportId: reportId)
    }

    func onReportDetailClosed() {
        viewModel.onReportDetailClosed()
    }

    func onReportThreadOpened() {
        viewModel.onReportThreadOpened()
    }

    func onReportCommentChanged(_ comment: String) {
        viewModel.onReportCommentChanged(comment: comment)
    }

    func onReportUrgencySelected(_ label: String) {
        viewModel.onReportUrgencySelected(label: label)
    }

    func onReportImageToggleChanged(_ isEnabled: Bool) {
        viewModel.onReportImageToggleChanged(isEnabled: isEnabled)
    }

    func onReportLocationToggleChanged(_ isEnabled: Bool) {
        viewModel.onReportLocationToggleChanged(isEnabled: isEnabled)
    }

    func onReportContinueToPlaceSelection() {
        viewModel.onReportContinueToPlaceSelection()
    }

    func onReportPlaceSelected(_ placeId: String) {
        viewModel.onReportPlaceSelected(placeId: placeId)
    }

    func onReportSubmitted() {
        viewModel.onReportSubmitted()
    }

    func onReportBack() {
        viewModel.onReportBack()
    }

    func onContactThreadSelected(_ threadId: String) {
        viewModel.onContactThreadSelected(threadId: threadId)
    }

    func onContactReportMessageSelected(_ reportId: String) {
        viewModel.onContactReportMessageSelected(reportId: reportId)
    }

    func onContactBackToList() {
        viewModel.onContactBackToList()
    }

    func onContactNewThreadStarted() {
        viewModel.onContactNewThreadStarted()
    }

    func onContactTargetTypeSelected(_ type: ContactTargetType) {
        viewModel.onContactTargetTypeSelected(type: type)
    }

    func onContactTargetSelected(_ targetId: String) {
        viewModel.onContactTargetSelected(targetId: targetId)
    }

    func onContactDraftChanged(_ text: String) {
        viewModel.onContactDraftChanged(text: text)
    }

    func onContactSendClicked() {
        viewModel.onContactSendClicked()
    }

    deinit {
        stateJob?.cancel(cause: nil)
        viewModel.clear()
    }
}
