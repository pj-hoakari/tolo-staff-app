import SwiftUI
import SharedLogic

struct ReportsTabRootView: View {
    private enum ReportRoute: Hashable {
        case draft
        case placeSelection
        case thread
    }

    let state: ReportsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onTypeSelected: (String) -> Void = { _ in }
    var onCommentChanged: (String) -> Void = { _ in }
    var onUrgencySelected: (String) -> Void = { _ in }
    var onImageToggleChanged: (Bool) -> Void = { _ in }
    var onLocationToggleChanged: (Bool) -> Void = { _ in }
    var onContinueToPlaceSelection: () -> Void = {}
    var onPlaceSelected: (String) -> Void = { _ in }
    var onSubmitted: () -> Void = {}
    var onBack: () -> Void = {}

    var body: some View {
        NavigationStack(path: navigationPath) {
            reportTypeSelection
                .navigationTitle("app_shell_reports_title")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                        CurrentStaffHeaderIconView(currentStaff: currentStaff)
                    }
                }
                .navigationDestination(for: ReportRoute.self) { route in
                    switch route {
                    case .draft:
                        reportDraftInput
                            .navigationTitle("報告内容入力")
                            .navigationBarTitleDisplayMode(.inline)
                    case .placeSelection:
                        reportPlaceSelection
                            .navigationTitle("対象場所")
                            .navigationBarTitleDisplayMode(.inline)
                    case .thread:
                        reportThread
                            .navigationTitle("報告スレッド")
                            .navigationBarTitleDisplayMode(.inline)
                    }
                }
        }
    }

    private var navigationPath: Binding<[ReportRoute]> {
        Binding(
            get: { navigationRoutes },
            set: { newValue in
                guard newValue.count < navigationRoutes.count else { return }
                for _ in 0..<(navigationRoutes.count - newValue.count) {
                    onBack()
                }
            }
        )
    }

    private var navigationRoutes: [ReportRoute] {
        switch state.step {
        case .typeSelection:
            []
        case .draftInput:
            [.draft]
        case .placeSelection:
            [.draft, .placeSelection]
        case .thread:
            [.draft, .placeSelection, .thread]
        }
    }

    private var reportTypeSelection: some View {
        List(state.reportTypes, id: \.id) { type in
                Button {
                    onTypeSelected(type.id)
                } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(type.title)
                            .font(.headline)
                            .foregroundStyle(.primary)
                        Text(type.detailText)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("report_type_\(type.id)")
        }
    }

    private var reportDraftInput: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("定型文")
                    .font(.headline)
                Text(state.draft.templateText)
                    .foregroundStyle(.secondary)

                Text("コメント")
                    .font(.headline)
                TextField(
                    "現場の状況を入力",
                    text: Binding(
                        get: { state.draft.comment },
                        set: onCommentChanged
                    ),
                    axis: .vertical
                )
                .textFieldStyle(.roundedBorder)

                Text("緊急度")
                    .font(.headline)
                Picker(
                    "緊急度",
                    selection: Binding(
                        get: { state.draft.urgencyLabel },
                        set: onUrgencySelected
                    )
                ) {
                    Text("通常").tag("通常")
                    Text("高").tag("高")
                    Text("緊急").tag("緊急")
                }
                .pickerStyle(.segmented)

                Toggle("画像を添付", isOn: Binding(
                    get: { state.draft.includesImage },
                    set: onImageToggleChanged
                ))
                Toggle("位置情報を添付", isOn: Binding(
                    get: { state.draft.includesLocation },
                    set: onLocationToggleChanged
                ))

                Button("対象場所を選ぶ", action: onContinueToPlaceSelection)
                    .buttonStyle(.borderedProminent)
            }
            .padding(16)
        }
        .accessibilityIdentifier("report_draft_input")
    }

    private var reportPlaceSelection: some View {
        List(state.availablePlaces, id: \.id) { place in
            Button {
                onPlaceSelected(place.id)
            } label: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(place.displayName)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    if let subtitle = place.subtitle {
                        Text(subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .safeAreaInset(edge: .bottom) {
            HStack {
                Button("本部へ送信", action: onSubmitted)
                    .buttonStyle(.borderedProminent)
                    .disabled(state.draft.selectedPlaceId == nil)
            }
            .padding()
            .background(.thinMaterial)
        }
        .accessibilityIdentifier("report_place_selection")
    }

    private var reportThread: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(state.submittedThread?.title ?? "報告スレッド")
                    .font(.title2)
                    .fontWeight(.bold)
                Text(state.submittedThread?.targetLabel ?? "")
                    .foregroundStyle(.secondary)
                Text(state.submittedThread?.lastSubmittedSummary ?? "")
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.blue.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                ForEach(state.submittedThread?.messages ?? [], id: \.id) { message in
                    ThreadMessageBubble(message: message)
                }
            }
            .padding(16)
        }
        .accessibilityIdentifier("report_thread")
    }
}
