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
        TabView(
            selection: Binding(
                get: { state.selectedTab },
                set: { onTabSelected($0) }
            )
        ) {
            tabPage(
                tab: AppTab.home,
                titleKey: "app_shell_home_title",
                descriptionKey: "app_shell_home_description",
                systemImage: "house.fill"
            )

            tabPage(
                tab: AppTab.instructions,
                titleKey: "app_shell_instructions_title",
                descriptionKey: "app_shell_instructions_description",
                systemImage: "checklist"
            )

            tabPage(
                tab: AppTab.reports,
                titleKey: "app_shell_reports_title",
                descriptionKey: "app_shell_reports_description",
                systemImage: "doc.text.fill"
            )

            contactTabPage()
        }
    }

    private func tabPage(
        tab: AppTab,
        titleKey: LocalizedStringKey,
        descriptionKey: LocalizedStringKey,
        systemImage: String
    ) -> some View {
        NavigationStack {
            AppShellTabContentView(
                placementName: state.currentPlacementName,
                titleKey: titleKey,
                descriptionKey: descriptionKey,
                selectedTab: tab
            )
            .navigationTitle(titleKey)
        }
        .tabItem {
            Label(tab.labelKey, systemImage: systemImage)
        }
        .tag(tab)
    }

    private func contactTabPage() -> some View {
        NavigationStack {
            ContactChatScreen()
        }
        .tabItem {
            Label(AppTab.contacts.labelKey, systemImage: "message.fill")
        }
        .tag(AppTab.contacts)
    }
}

private struct AppShellTabContentView: View {
    let placementName: String
    let titleKey: LocalizedStringKey
    let descriptionKey: LocalizedStringKey
    let selectedTab: AppTab

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 12) {
                Text(titleKey)
                    .font(.title)
                    .fontWeight(.semibold)
                    .accessibilityIdentifier(selectedTab.contentTitleIdentifier)

                Text(descriptionKey)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            placementHeader
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
    }

    private var placementHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("app_shell_placement_header")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(placementName)
                .font(.title2)
                .fontWeight(.semibold)
                .accessibilityIdentifier("app_shell_placement_name")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 8))
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
        default:
            return "app_shell_tab_home"
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
        default:
            return "app_shell_content_home_title"
        }
    }
}

#Preview {
    AppShellContentView(
        state: AppShellUiState(
            currentPlacementName: "東京会場",
            selectedTab: AppTab.home
        )
    )
}
