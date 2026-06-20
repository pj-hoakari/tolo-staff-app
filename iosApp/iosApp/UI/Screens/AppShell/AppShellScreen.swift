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
            onPlacementChangeConfirmed: wrapper.onPlacementChangeConfirmed,
            onPlacementArrivalConfirmed: wrapper.onPlacementArrivalConfirmed,
            onOpenReportFromHome: { wrapper.onTabSelected(.reports) },
            onOpenContactsFromHome: { wrapper.onTabSelected(.contacts) },
            onInstructionSelected: wrapper.onInstructionSelected,
            onInstructionThreadOpened: wrapper.onInstructionThreadOpened,
            onInstructionDetailClosed: wrapper.onInstructionDetailClosed,
            onInstructionStatusUpdated: wrapper.onInstructionStatusUpdated,
            onReportTypeSelected: wrapper.onReportTypeSelected,
            onReportSelected: wrapper.onReportSelected,
            onReportDetailClosed: wrapper.onReportDetailClosed,
            onReportThreadOpened: wrapper.onReportThreadOpened,
            onReportCommentChanged: wrapper.onReportCommentChanged,
            onReportUrgencySelected: wrapper.onReportUrgencySelected,
            onReportImageToggleChanged: wrapper.onReportImageToggleChanged,
            onReportLocationToggleChanged: wrapper.onReportLocationToggleChanged,
            onReportContinueToPlaceSelection: wrapper.onReportContinueToPlaceSelection,
            onReportPlaceSelected: wrapper.onReportPlaceSelected,
            onReportSubmitted: wrapper.onReportSubmitted,
            onReportBack: wrapper.onReportBack,
            onContactThreadSelected: wrapper.onContactThreadSelected,
            onContactReportMessageSelected: wrapper.onContactReportMessageSelected,
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
    var onPlacementChangeConfirmed: () -> Void = {}
    var onPlacementArrivalConfirmed: () -> Void = {}
    var onOpenReportFromHome: () -> Void = {}
    var onOpenContactsFromHome: () -> Void = {}
    var onInstructionSelected: (String) -> Void = { _ in }
    var onInstructionThreadOpened: () -> Void = {}
    var onInstructionDetailClosed: () -> Void = {}
    var onInstructionStatusUpdated: (InstructionProgressStatus) -> Void = { _ in }
    var onReportTypeSelected: (String) -> Void = { _ in }
    var onReportSelected: (String) -> Void = { _ in }
    var onReportDetailClosed: () -> Void = {}
    var onReportThreadOpened: () -> Void = {}
    var onReportCommentChanged: (String) -> Void = { _ in }
    var onReportUrgencySelected: (String) -> Void = { _ in }
    var onReportImageToggleChanged: (Bool) -> Void = { _ in }
    var onReportLocationToggleChanged: (Bool) -> Void = { _ in }
    var onReportContinueToPlaceSelection: () -> Void = {}
    var onReportPlaceSelected: (String) -> Void = { _ in }
    var onReportSubmitted: () -> Void = {}
    var onReportBack: () -> Void = {}
    var onContactThreadSelected: (String) -> Void = { _ in }
    var onContactReportMessageSelected: (String) -> Void = { _ in }
    var onContactBackToList: () -> Void = {}
    var onContactNewThreadStarted: () -> Void = {}
    var onContactTargetTypeSelected: (ContactTargetType) -> Void = { _ in }
    var onContactTargetSelected: (String) -> Void = { _ in }
    var onContactDraftChanged: (String) -> Void = { _ in }
    var onContactSendClicked: () -> Void = {}

    var body: some View {
        tabContainer
    }

    private var displayedSelectedTab: AppTab { state.displayedSelectedTab }

    @ViewBuilder
    private var tabContainer: some View {
        if #available(iOS 26.1, *) {
            tabs
                .tabViewBottomAccessory(isEnabled: displayedSelectedTab != AppTab.contacts) {
                    PlacementBar(
                        placementStatus: state.homeOverview.placementStatus,
                        onPlacementChangeConfirmed: onPlacementChangeConfirmed,
                        onPlacementArrivalConfirmed: onPlacementArrivalConfirmed
                    )
                }
        } else if #available(iOS 26.0, *) {
            tabs
                .tabViewBottomAccessory {
                    if displayedSelectedTab != AppTab.contacts {
                        PlacementBar(
                            placementStatus: state.homeOverview.placementStatus,
                            onPlacementChangeConfirmed: onPlacementChangeConfirmed,
                            onPlacementArrivalConfirmed: onPlacementArrivalConfirmed
                        )
                    }
                }
        } else {
            tabs
        }
    }

    private var tabs: some View {
        TabView(
            selection: Binding(
                get: { displayedSelectedTab },
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
                    selectedThread: state.contactsTab.selectedThread,
                    selectedThreadBackDestination: state.contactsTab.selectedThreadBackDestination,
                    onTypeSelected: onReportTypeSelected,
                    onReportSelected: onReportSelected,
                    onReportDetailClosed: onReportDetailClosed,
                    onReportThreadOpened: onReportThreadOpened,
                    onCommentChanged: onReportCommentChanged,
                    onUrgencySelected: onReportUrgencySelected,
                    onImageToggleChanged: onReportImageToggleChanged,
                    onLocationToggleChanged: onReportLocationToggleChanged,
                    onContinueToPlaceSelection: onReportContinueToPlaceSelection,
                    onPlaceSelected: onReportPlaceSelected,
                    onSubmitted: onReportSubmitted,
                    onBack: onReportBack,
                    onContactBackToList: onContactBackToList,
                    onContactDraftChanged: onContactDraftChanged,
                    onContactSendClicked: onContactSendClicked
                )
            }

            Tab("app_shell_tab_contacts", systemImage: "message.fill", value: AppTab.contacts) {
                ContactsTabRootView(
                    state: state.contactsTab,
                    currentStaff: state.currentStaff,
                    onThreadSelected: onContactThreadSelected,
                    onReportSelected: onContactReportMessageSelected,
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
    let placementStatus: PlacementStatusUiModel
    var onPlacementChangeConfirmed: () -> Void = {}
    var onPlacementArrivalConfirmed: () -> Void = {}

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbolName)
                .font(.title3.weight(.semibold))
                .foregroundStyle(iconColor)
                .symbolEffect(.bounce, value: placementStatus.phase)

            VStack(alignment: .leading, spacing: 4) {
                Text(placementStatus.headline)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                if placementStatus.phase == .active {
                    Text(placementStatus.placementName)
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("app_shell_placement_name")
                } else {
                    Text(placementStatus.placementName)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("app_shell_placement_name")
                }
            }

            Spacer(minLength: 12)

            if placementStatus.showsActionButton, let buttonLabel = placementStatus.buttonLabel {
                Button(action: handleButtonTap) {
                    Label(buttonLabel, systemImage: buttonSymbolName)
                        .font(.footnote.weight(.semibold))
                }
                .buttonStyle(PlacementActionButtonStyle(phase: placementStatus.phase))
                .transition(
                    .asymmetric(
                        insertion: .move(edge: .bottom).combined(with: .opacity),
                        removal: .opacity
                    )
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(backgroundStyle)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(borderColor)
                .frame(height: 1)
        }
        .contentShape(Rectangle())
        .id("\(placementStatus.assignId ?? "none")-\(placementStatus.phase)")
        .transition(
            .asymmetric(
                insertion: .move(edge: .bottom).combined(with: .opacity),
                removal: .opacity
            )
        )
        .animation(.spring(duration: 0.45, bounce: 0.14), value: placementStatus.phase)
    }

    private var symbolName: String {
        switch placementStatus.phase {
        case .pendingChange:
            return "bell.badge"
        case .enRoute:
            return "figure.walk.motion"
        case .active:
            return "mappin.circle.fill"
        default:
            return "mappin.circle.fill"
        }
    }

    private var buttonSymbolName: String {
        switch placementStatus.phase {
        case .pendingChange:
            return "checkmark.circle.fill"
        case .enRoute:
            return "flag.checkered.circle.fill"
        case .active:
            return "checkmark.circle.fill"
        default:
            return "checkmark.circle.fill"
        }
    }

    private var iconColor: Color {
        switch placementStatus.phase {
        case .pendingChange:
            return Color.orange
        case .enRoute:
            return Color.accentColor
        case .active:
            return Color.accentColor
        default:
            return Color.accentColor
        }
    }

    private var backgroundStyle: AnyShapeStyle {
        switch placementStatus.phase {
        case .pendingChange:
            return AnyShapeStyle(Color.orange.opacity(0.16))
        case .enRoute:
            return AnyShapeStyle(Color.accentColor.opacity(0.14))
        case .active:
            return AnyShapeStyle(Color.clear)
        default:
            return AnyShapeStyle(Color.clear)
        }
    }

    private var borderColor: Color {
        switch placementStatus.phase {
        case .pendingChange:
            return Color.orange.opacity(0.18)
        case .enRoute:
            return Color.accentColor.opacity(0.16)
        case .active:
            return Color.primary.opacity(0.08)
        default:
            return Color.primary.opacity(0.08)
        }
    }

    private func handleButtonTap() {
        switch placementStatus.phase {
        case .pendingChange:
            onPlacementChangeConfirmed()
        case .enRoute:
            onPlacementArrivalConfirmed()
        case .active:
            break
        default:
            break
        }
    }
}

private struct PlacementActionButtonStyle: ButtonStyle {
    let phase: PlacementPhase

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(buttonBackground, in: Capsule())
            .foregroundStyle(buttonForeground)
            .scaleEffect(configuration.isPressed ? 0.96 : 1.0)
            .animation(.spring(duration: 0.22, bounce: 0.28), value: configuration.isPressed)
    }

    private var buttonBackground: Color {
        switch phase {
        case .pendingChange:
            return Color.orange
        case .enRoute:
            return Color.accentColor
        case .active:
            return Color.accentColor
        default:
            return Color.accentColor
        }
    }

    private var buttonForeground: Color {
        Color.white
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
