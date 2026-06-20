import SwiftUI
import SharedLogic

struct ReportsTabRootView: View {
    private enum ReportRoute: Hashable {
        case draft
        case placeSelection
        case detail
        case thread
    }

    let state: ReportsTabUiState
    let currentStaff: CurrentStaffUiModel
    let selectedThread: ContactThreadDetailUiModel?
    let selectedThreadBackDestination: ContactThreadBackDestination
    var onTypeSelected: (String) -> Void = { _ in }
    var onReportSelected: (String) -> Void = { _ in }
    var onReportDetailClosed: () -> Void = {}
    var onReportThreadOpened: () -> Void = {}
    var onCommentChanged: (String) -> Void = { _ in }
    var onUrgencySelected: (String) -> Void = { _ in }
    var onImageToggleChanged: (Bool) -> Void = { _ in }
    var onLocationToggleChanged: (Bool) -> Void = { _ in }
    var onContinueToPlaceSelection: () -> Void = {}
    var onPlaceSelected: (String) -> Void = { _ in }
    var onSubmitted: () -> Void = {}
    var onBack: () -> Void = {}
    var onContactBackToList: () -> Void = {}
    var onContactDraftChanged: (String) -> Void = { _ in }
    var onContactSendClicked: () -> Void = {}

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
                    case .detail:
                        if let selectedReport = state.selectedReport {
                            ReportDetailView(
                                report: selectedReport,
                                onOpenThread: onReportThreadOpened
                            )
                            .navigationTitle("報告詳細")
                            .navigationBarTitleDisplayMode(.inline)
                        }
                    case .thread:
                        if let selectedThread {
                            ContactThreadDetailView(
                                thread: selectedThread,
                                onReportSelected: { _ in },
                                onDraftChanged: onContactDraftChanged,
                                onSendClicked: onContactSendClicked
                            )
                            .navigationTitle(selectedThread.title)
                            .navigationBarTitleDisplayMode(.inline)
                        }
                    }
                }
        }
    }

    private var navigationPath: Binding<[ReportRoute]> {
        Binding(
            get: { navigationRoutes },
            set: { newValue in
                guard newValue.count < navigationRoutes.count else { return }
                let removedRoutes = navigationRoutes.dropFirst(newValue.count).reversed()
                for route in removedRoutes {
                    switch route {
                    case .draft, .placeSelection:
                        onBack()
                    case .detail:
                        onReportDetailClosed()
                    case .thread:
                        onContactBackToList()
                    }
                }
            }
        )
    }

    private var navigationRoutes: [ReportRoute] {
        var routes: [ReportRoute] = []

        switch state.step {
        case .typeSelection:
            break
        case .draftInput:
            routes.append(.draft)
        case .placeSelection:
            routes.append(.draft)
            routes.append(.placeSelection)
        }

        if state.selectedReport != nil {
            routes.append(.detail)
        }

        if selectedThreadBackDestination == .reportDetail,
           selectedThread != nil {
            routes.append(.thread)
        }

        return routes
    }

    private var reportTypeSelection: some View {
        List {
            Section("報告種別") {
                ForEach(state.reportTypes, id: \.id) { type in
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

            Section("関連報告一覧") {
                if state.isLoadingReports {
                    ProgressView("関連報告を読み込んでいます")
                } else if let reportsErrorMessage = state.reportsErrorMessage {
                    Text(reportsErrorMessage)
                        .foregroundStyle(.red)
                } else if state.relatedReports.isEmpty {
                    Text("関連する報告はまだありません")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(state.relatedReports, id: \.reportId) { report in
                        Button {
                            onReportSelected(report.reportId)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack(alignment: .top) {
                                    Text(report.title)
                                        .font(.headline)
                                        .foregroundStyle(.primary)
                                    Spacer()
                                    if report.isAuthoredByCurrentStaff {
                                        Text("自分の報告")
                                            .font(.caption)
                                            .foregroundStyle(.tint)
                                    }
                                }
                                if !report.priorityLabel.isEmpty {
                                    Text(report.priorityLabel)
                                        .font(.caption)
                                        .foregroundStyle(.tint)
                                }
                                Text(report.summary)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                Text("\(report.targetLabel) / \(report.authorName)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                if let timeLabel = report.timeLabel {
                                    Text(timeLabel)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .padding(.vertical, 6)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("related_report_\(report.reportId)")
                    }
                }
            }
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
}

struct ReportDetailView: View {
    let report: ReportDetailUiModel
    let onOpenThread: () -> Void

    var body: some View {
        List {
            Section("概要") {
                VStack(alignment: .leading, spacing: 8) {
                    Text(report.title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                        .accessibilityIdentifier("report_detail_title")
                    if !report.priorityLabel.isEmpty {
                        Text(report.priorityLabel)
                            .font(.caption)
                            .foregroundStyle(.tint)
                    }
                    Text(report.summary)
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("report_detail_summary")
                    Text("\(report.targetLabel) / \(report.authorName)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if let timeLabel = report.timeLabel {
                        Text(timeLabel)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
            }

            Section("補足") {
                Text(report.detailPlaceholderMessage)
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier("report_detail_placeholder")
            }

            Section {
                Button("連絡スレッドを開く", action: onOpenThread)
                    .accessibilityIdentifier("report_detail_open_thread_button")
            }
        }
        .accessibilityIdentifier("report_detail_screen")
    }
}
