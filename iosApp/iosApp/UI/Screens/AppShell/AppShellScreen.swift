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
            onTabSelected: { wrapper.onTabSelected($0) }
        )
    }
}

struct AppShellContentView: View {
    let state: AppShellUiState
    var onTabSelected: (AppTab) -> Void = { _ in }

    var body: some View {
        if #available(iOS 26.1, *) {
            appTabs
                .tabViewBottomAccessory(isEnabled: state.selectedTab != AppTab.contacts) {
                    PlacementBar(placementName: state.currentPlacementName)
                }
        } else if #available(iOS 26.0, *) {
            appTabs
                .tabViewBottomAccessory {
                    if state.selectedTab != AppTab.contacts {
                        PlacementBar(placementName: state.currentPlacementName)
                    }
                }
        } else {
            appTabs
        }
    }

    private var appTabs: some View {
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
                        isLoading: state.isLoading,
                        errorMessage: state.errorMessage
                    )
                        .navigationTitle("app_shell_home_title")
                }
            }

            Tab("app_shell_tab_instructions", systemImage: "checklist", value: AppTab.instructions) {
                tabPage(
                    tab: AppTab.instructions,
                    titleKey: "app_shell_instructions_title",
                    descriptionKey: "app_shell_instructions_description"
                )
            }

            Tab("app_shell_tab_reports", systemImage: "doc.text.fill", value: AppTab.reports) {
                tabPage(
                    tab: AppTab.reports,
                    titleKey: "app_shell_reports_title",
                    descriptionKey: "app_shell_reports_description"
                )
            }

            Tab("app_shell_tab_contacts", systemImage: "message.fill", value: AppTab.contacts) {
                NavigationStack {
                    ContactChatScreen()
                }
            }
        }
    }

    private func tabPage(
        tab: AppTab,
        titleKey: LocalizedStringKey,
        descriptionKey: LocalizedStringKey
    ) -> some View {
        NavigationStack {
            AppShellTabContentView(
                descriptionKey: descriptionKey,
                selectedTab: tab
            )
            .navigationTitle(titleKey)
        }
    }
}

private struct AppShellTabContentView: View {
    let descriptionKey: LocalizedStringKey
    let selectedTab: AppTab

    var body: some View {
        List {
            Section {
                Text(descriptionKey)
                    .accessibilityIdentifier(selectedTab.contentTitleIdentifier)
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

private extension AppTab {
    var labelKey: LocalizedStringKey {
        switch self {
        case .home:
            return "app_shell_tab_home"
        case .instructions:
            return "app_shell_tab_instructions"
        case .reports:
            return "app_shell_tab_reports"
        case .contacts:
            return "app_shell_tab_contacts"
        }
    }

    var contentTitleIdentifier: String {
        switch self {
        case .home:
            return "app_shell_content_home_title"
        case .instructions:
            return "app_shell_content_instructions_title"
        case .reports:
            return "app_shell_content_reports_title"
        case .contacts:
            return "app_shell_content_contacts_title"
        }
    }
}

#Preview {
    AppShellContentView(
        state: AppShellUiState.mock()
    )
}
