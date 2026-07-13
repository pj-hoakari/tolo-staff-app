import SwiftUI
import SharedLogic

struct ReportsTabRootView: View {
    private enum Route: Hashable { case draft, place, detail }

    let state: ReportsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onTypeSelected: (String) -> Void = { _ in }
    var onReportSelected: (String) -> Void = { _ in }
    var onReportDetailClosed: () -> Void = {}
    var onCommentChanged: (String) -> Void = { _ in }
    var onUrgencySelected: (String) -> Void = { _ in }
    var onContinueToPlaceSelection: () -> Void = {}
    var onPlaceSelected: (String) -> Void = { _ in }
    var onSubmitted: () -> Void = {}
    var onBack: () -> Void = {}

    var body: some View {
        NavigationStack(path: navigationPath) {
            reportTypeSelection
                .navigationTitle("報告")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                        CurrentStaffHeaderIconView(currentStaff: currentStaff)
                    }
                }
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .draft: reportDraft
                    case .place: placeSelection
                    case .detail:
                        if let report = state.selectedReport { ReportDetailView(report: report) }
                    }
                }
        }
    }

    private var navigationPath: Binding<[Route]> {
        Binding(
            get: {
                if state.selectedReport != nil { return [.detail] }
                switch state.step {
                case .typeSelection: return []
                case .draftInput: return [.draft]
                case .placeSelection: return [.draft, .place]
                default: return []
                }
            },
            set: { path in
                if path.isEmpty, state.selectedReport != nil { onReportDetailClosed() }
                else { onBack() }
            }
        )
    }

    private var reportTypeSelection: some View {
        List {
            Section("報告種別") {
                ForEach(state.reportTypes, id: \.id) { type in
                    Button {
                        onTypeSelected(type.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(type.title).font(.headline).foregroundStyle(.primary)
                            Text(type.detailText).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    .accessibilityIdentifier("report_type_\(type.id)")
                }
            }
            Section("関連報告一覧") {
                if state.isLoadingReports {
                    ProgressView("関連報告を読み込んでいます")
                } else if let error = state.reportsErrorMessage {
                    Text(error).foregroundStyle(.red)
                } else if state.relatedReports.isEmpty {
                    Text("関連する報告はまだありません").foregroundStyle(.secondary)
                } else {
                    ForEach(state.relatedReports, id: \.reportId) { report in
                        Button { onReportSelected(report.reportId) } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(report.title).font(.headline).foregroundStyle(.primary)
                                    Spacer()
                                    if report.isAuthoredByCurrentStaff { Text("自分の報告").font(.caption) }
                                }
                                Text(report.severityLabel).font(.caption).foregroundStyle(.tint)
                                Text(report.summary).font(.subheadline).foregroundStyle(.secondary)
                                Text("\(report.targetLabel) / \(report.authorName)").font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        .accessibilityIdentifier("related_report_\(report.reportId)")
                    }
                }
            }
        }
        .accessibilityIdentifier("report_type_list")
    }

    private var reportDraft: some View {
        Form {
            Section("コメント") {
                TextField(
                    "現場の状況を入力",
                    text: Binding(get: { state.draft.comment }, set: onCommentChanged),
                    axis: .vertical
                )
            }
            Section("緊急度") {
                Picker(
                    "緊急度",
                    selection: Binding(get: { state.draft.urgencyLabel }, set: onUrgencySelected)
                ) {
                    Text("通常").tag("通常")
                    Text("高").tag("高")
                    Text("緊急").tag("緊急")
                }
                .pickerStyle(.segmented)
            }
            Button("対象場所を選ぶ", action: onContinueToPlaceSelection)
                .buttonStyle(.borderedProminent)
        }
        .navigationTitle("報告内容入力")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("report_draft_input")
    }

    private var placeSelection: some View {
        List {
            ForEach(state.availablePlaces, id: \.id) { place in
                Button(place.displayName) { onPlaceSelected(place.id) }
                    .foregroundStyle(.primary)
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button("本部へ送信", action: onSubmitted)
                .buttonStyle(.borderedProminent)
                .disabled(state.draft.selectedPlaceId == nil)
                .padding()
        }
        .navigationTitle("対象場所")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("report_place_selection")
    }
}

private struct ReportDetailView: View {
    let report: ReportDetailUiModel

    var body: some View {
        List {
            Section("概要") {
                Text(report.title).font(.headline).accessibilityIdentifier("report_detail_title")
                Text(report.severityLabel).font(.caption).foregroundStyle(.tint)
                Text(report.statusLabel).font(.caption).foregroundStyle(.secondary)
                Text(report.summary).accessibilityIdentifier("report_detail_summary")
                Text("\(report.targetLabel) / \(report.authorName)").font(.caption).foregroundStyle(.secondary)
            }
        }
        .navigationTitle("報告詳細")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("report_detail_screen")
    }
}
