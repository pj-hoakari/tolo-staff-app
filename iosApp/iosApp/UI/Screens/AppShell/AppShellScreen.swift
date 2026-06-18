import SwiftUI
import SharedLogic

struct AppShellScreen: View {
    @StateObject private var wrapper: AppShellViewModelWrapper

    init(wrapper: AppShellViewModelWrapper = AppShellViewModelWrapper()) {
        _wrapper = StateObject(wrappedValue: wrapper)
    }

    var body: some View {
        AppShellContentView(
            state: wrapper.state,
            onTabSelected: wrapper.onTabSelected,
            onHomeInstructionSelected: wrapper.onHomeInstructionSelected,
            onOpenReportFromHome: { wrapper.onTabSelected(.reports) },
            onOpenContactsFromHome: { wrapper.onTabSelected(.contacts) },
            onInstructionSelected: wrapper.onInstructionSelected,
            onInstructionThreadOpened: wrapper.onInstructionThreadOpened,
            onInstructionDetailClosed: wrapper.onInstructionDetailClosed,
            onInstructionStatusUpdated: wrapper.onInstructionStatusUpdated,
            onReportTypeSelected: wrapper.onReportTypeSelected,
            onReportCommentChanged: wrapper.onReportCommentChanged,
            onReportUrgencySelected: wrapper.onReportUrgencySelected,
            onReportImageToggleChanged: wrapper.onReportImageToggleChanged,
            onReportLocationToggleChanged: wrapper.onReportLocationToggleChanged,
            onReportContinueToPlaceSelection: wrapper.onReportContinueToPlaceSelection,
            onReportPlaceSelected: wrapper.onReportPlaceSelected,
            onReportSubmitted: wrapper.onReportSubmitted,
            onReportBack: wrapper.onReportBack,
            onContactThreadSelected: wrapper.onContactThreadSelected,
            onContactBackToList: wrapper.onContactBackToList,
            onContactNewThreadStarted: wrapper.onContactNewThreadStarted,
            onContactTargetTypeSelected: wrapper.onContactTargetTypeSelected,
            onContactTargetSelected: wrapper.onContactTargetSelected,
            onContactDraftChanged: wrapper.onContactDraftChanged,
            onContactSendClicked: wrapper.onContactSendClicked
        )
    }
}

struct AppShellContentView: View {
    let state: AppShellUiState
    var onTabSelected: (AppTab) -> Void = { _ in }
    var onHomeInstructionSelected: () -> Void = {}
    var onOpenReportFromHome: () -> Void = {}
    var onOpenContactsFromHome: () -> Void = {}
    var onInstructionSelected: (String) -> Void = { _ in }
    var onInstructionThreadOpened: () -> Void = {}
    var onInstructionDetailClosed: () -> Void = {}
    var onInstructionStatusUpdated: (InstructionProgressStatus) -> Void = { _ in }
    var onReportTypeSelected: (String) -> Void = { _ in }
    var onReportCommentChanged: (String) -> Void = { _ in }
    var onReportUrgencySelected: (String) -> Void = { _ in }
    var onReportImageToggleChanged: (Bool) -> Void = { _ in }
    var onReportLocationToggleChanged: (Bool) -> Void = { _ in }
    var onReportContinueToPlaceSelection: () -> Void = {}
    var onReportPlaceSelected: (String) -> Void = { _ in }
    var onReportSubmitted: () -> Void = {}
    var onReportBack: () -> Void = {}
    var onContactThreadSelected: (String) -> Void = { _ in }
    var onContactBackToList: () -> Void = {}
    var onContactNewThreadStarted: () -> Void = {}
    var onContactTargetTypeSelected: (ContactTargetType) -> Void = { _ in }
    var onContactTargetSelected: (String) -> Void = { _ in }
    var onContactDraftChanged: (String) -> Void = { _ in }
    var onContactSendClicked: () -> Void = {}

    var body: some View {
        if #available(iOS 26.1, *) {
            tabs
                .tabViewBottomAccessory(isEnabled: state.selectedTab != AppTab.contacts) {
                    PlacementBar(placementName: state.currentPlacementName)
                }
        } else if #available(iOS 26.0, *) {
            tabs
                .tabViewBottomAccessory {
                    if state.selectedTab != AppTab.contacts {
                        PlacementBar(placementName: state.currentPlacementName)
                    }
                }
        } else {
            tabs
        }
    }

    private var tabs: some View {
        TabView(
            selection: Binding(
                get: { state.selectedTab },
                set: { onTabSelected($0) }
            )
        ) {
            Tab("app_shell_tab_home", systemImage: "house.fill", value: AppTab.home) {
                NavigationStack {
                    HomeTabContentView(
                        overview: state.homeOverview,
                        errorMessage: state.errorMessage,
                        onOpenInstruction: onHomeInstructionSelected,
                        onOpenReport: onOpenReportFromHome,
                        onOpenContacts: onOpenContactsFromHome
                    )
                    .navigationTitle("app_shell_home_title")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                            CurrentStaffHeaderIconView(currentStaff: state.currentStaff)
                        }
                    }
                }
            }

            Tab("app_shell_tab_instructions", systemImage: "checklist", value: AppTab.instructions) {
                InstructionsTabRootView(
                    state: state.instructionsTab,
                    currentStaff: state.currentStaff,
                    onInstructionSelected: onInstructionSelected,
                    onThreadOpened: onInstructionThreadOpened,
                    onDetailClosed: onInstructionDetailClosed,
                    onStatusUpdated: onInstructionStatusUpdated
                )
            }

            Tab("app_shell_tab_reports", systemImage: "doc.text.fill", value: AppTab.reports) {
                ReportsTabRootView(
                    state: state.reportsTab,
                    currentStaff: state.currentStaff,
                    onTypeSelected: onReportTypeSelected,
                    onCommentChanged: onReportCommentChanged,
                    onUrgencySelected: onReportUrgencySelected,
                    onImageToggleChanged: onReportImageToggleChanged,
                    onLocationToggleChanged: onReportLocationToggleChanged,
                    onContinueToPlaceSelection: onReportContinueToPlaceSelection,
                    onPlaceSelected: onReportPlaceSelected,
                    onSubmitted: onReportSubmitted,
                    onBack: onReportBack
                )
            }

            Tab("app_shell_tab_contacts", systemImage: "message.fill", value: AppTab.contacts) {
                ContactsTabRootView(
                    state: state.contactsTab,
                    currentStaff: state.currentStaff,
                    onThreadSelected: onContactThreadSelected,
                    onBackToList: onContactBackToList,
                    onNewThreadStarted: onContactNewThreadStarted,
                    onTargetTypeSelected: onContactTargetTypeSelected,
                    onTargetSelected: onContactTargetSelected,
                    onDraftChanged: onContactDraftChanged,
                    onSendClicked: onContactSendClicked
                )
            }
        }
    }
}

struct PlacementBar: View {
    let placementName: String

    var body: some View {
        LabeledContent("app_shell_placement_header") {
            Text(placementName)
                .fontWeight(.semibold)
                .accessibilityIdentifier("app_shell_placement_name")
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(.bar)
    }
}

struct CurrentStaffHeaderIconView: View {
    let currentStaff: CurrentStaffUiModel

    var body: some View {
        Image(systemName: "person.crop.circle")
            .font(.title3)
            .frame(width: 28, height: 28)
            .foregroundStyle(Color.accentColor)
            .contentTransition(.identity)
            .transaction { transaction in
                transaction.animation = nil
            }
            .accessibilityIdentifier("current_staff_\(currentStaff.staffId)")
            .accessibilityLabel("current_staff_\(currentStaff.staffId)")
    }
}
