import SwiftUI
import SharedLogic

struct ContactsTabRootView: View {
    let state: ContactsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onThreadSelected: (String) -> Void = { _ in }
    var onReportSelected: (String) -> Void = { _ in }
    var onBackToList: () -> Void = {}
    var onNewThreadStarted: () -> Void = {}
    var onTargetTypeSelected: (ContactTargetType) -> Void = { _ in }
    var onTargetSelected: (String) -> Void = { _ in }
    var onDraftChanged: (String) -> Void = { _ in }
    var onSendClicked: () -> Void = {}

    var body: some View {
        NavigationStack {
            ContactThreadListView(
                state: state,
                onThreadSelected: onThreadSelected
            )
            .navigationTitle("app_shell_contacts_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                    CurrentStaffHeaderIconView(currentStaff: currentStaff)
                }
                ToolbarItem(id: "contact-new-thread", placement: .topBarTrailing) {
                    Button("新しい連絡", systemImage: "square.and.pencil", action: onNewThreadStarted)
                        .accessibilityIdentifier("contact_new_thread_button")
                }
            }
            .navigationDestination(
                isPresented: Binding(
                    get: { state.isChoosingTargetType },
                    set: { isPresented in
                        if !isPresented {
                            onBackToList()
                        }
                    }
                )
            ) {
                ContactTargetSelectionView(
                    state: state,
                    onTargetTypeSelected: onTargetTypeSelected,
                    onTargetSelected: onTargetSelected
                )
                .navigationTitle("宛先を選択")
                .navigationBarTitleDisplayMode(.inline)
            }
            .navigationDestination(
                isPresented: Binding(
                    get: {
                        state.selectedThread != nil &&
                            state.selectedThreadBackDestination != .reportDetail
                    },
                    set: { isPresented in
                        if !isPresented {
                            onBackToList()
                        }
                    }
                )
            ) {
                if let selectedThread = state.selectedThread {
                    ContactThreadDetailView(
                        thread: selectedThread,
                        onReportSelected: onReportSelected,
                        onDraftChanged: onDraftChanged,
                        onSendClicked: onSendClicked
                    )
                    .navigationTitle(selectedThread.title)
                    .navigationBarTitleDisplayMode(.inline)
                }
            }
        }
    }
}

private struct ContactThreadListView: View {
    let state: ContactsTabUiState
    let onThreadSelected: (String) -> Void

    var body: some View {
        List {
            Section("現在の連絡") {
                ForEach(state.threads.filter { !$0.isFormerAssignment }, id: \.id) { thread in
                    contactThreadRow(thread)
                }
            }

            if !state.formerAssignments.isEmpty {
                Section("旧担当") {
                    ForEach(state.threads.filter(\.isFormerAssignment), id: \.id) { thread in
                        contactThreadRow(thread)
                    }
                    ForEach(state.formerAssignments, id: \.id) { assignment in
                        Text(assignment.summary)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .accessibilityIdentifier("contact_thread_list")
    }

    private func contactThreadRow(_ thread: ContactThreadSummaryUiModel) -> some View {
        Button {
            onThreadSelected(thread.id)
        } label: {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(thread.title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text(thread.lastMessagePreview)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Spacer()
                if thread.unreadCount > 0 {
                    Text("\(thread.unreadCount)")
                        .font(.caption)
                        .foregroundStyle(.white)
                        .padding(6)
                        .background(.blue, in: Circle())
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("contact_thread_\(thread.id)")
    }
}

private struct ContactTargetSelectionView: View {
    let state: ContactsTabUiState
    let onTargetTypeSelected: (ContactTargetType) -> Void
    let onTargetSelected: (String) -> Void

    private var filteredTargets: [ContactTargetUiModel] {
        guard let selectedTargetType = state.selectedTargetType else {
            return []
        }
        return state.availableTargets.filter { $0.type == selectedTargetType }
    }

    var body: some View {
        List {
            Section("宛先種別") {
                Button("担当場所") { onTargetTypeSelected(.place) }
                Button("担当ロール") { onTargetTypeSelected(.role) }
                Button("本部") { onTargetTypeSelected(.headquarters) }
                Button("個人") { onTargetTypeSelected(.user) }
            }

            if !filteredTargets.isEmpty {
                Section("宛先候補") {
                    ForEach(filteredTargets, id: \.id) { target in
                        Button {
                            onTargetSelected(target.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(target.displayName)
                                if let subtitle = target.subtitle {
                                    Text(subtitle)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        .accessibilityIdentifier("contact_target_selection")
    }
}

struct ContactThreadDetailView: View {
    let thread: ContactThreadDetailUiModel
    let onReportSelected: (String) -> Void
    let onDraftChanged: (String) -> Void
    let onSendClicked: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(thread.title)
                                .font(.title3)
                                .fontWeight(.bold)
                            Text(thread.target.displayName)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if thread.isFormerAssignment {
                            Text("旧担当")
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(.secondary.opacity(0.12), in: Capsule())
                        }
                    }

                    ForEach(thread.messages, id: \.id) { message in
                        ThreadMessageBubble(
                            message: message,
                            onReportSelected: onReportSelected
                        )
                    }
                }
                .padding(16)
            }

            HStack {
                TextField(
                    "メッセージ",
                    text: Binding(
                        get: { thread.draftMessage },
                        set: onDraftChanged
                    ),
                    axis: .vertical
                )
                .lineLimit(1 ... 4)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color(uiColor: .systemBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .strokeBorder(.quaternary, lineWidth: 1)
                )
                .accessibilityIdentifier("contact_chat_message_input")

                Button("Send", systemImage: "paperplane.fill", action: onSendClicked)
                    .labelStyle(.iconOnly)
                    .frame(width: 44, height: 44)
                    .background(Color.accentColor, in: Circle())
                    .foregroundStyle(.white)
                    .disabled(!thread.canReply || thread.draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .opacity(!thread.canReply || thread.draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.55 : 1)
                    .accessibilityIdentifier("contact_chat_send_button")
            }
            .padding()
            .background(.thinMaterial)
        }
        .accessibilityIdentifier("contact_thread_detail")
    }
}

struct ThreadMessageBubble: View {
    let message: ThreadMessageUiModel
    let onReportSelected: (String) -> Void

    var body: some View {
        HStack {
            if message.isCurrentUser {
                Spacer(minLength: 48)
            }

            VStack(alignment: message.isCurrentUser ? .trailing : .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(message.senderName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if let senderRoleLabel = message.senderRoleLabel {
                        Text(senderRoleLabel)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }

                if let reportId = message.reportId,
                   let reportTitle = message.reportTitle,
                   let reportSummary = message.reportSummary,
                   let reportAuthorName = message.reportAuthorName,
                   let reportTargetLabel = message.reportTargetLabel {
                    Button {
                        onReportSelected(reportId)
                    } label: {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("報告")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(.tint)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Text(reportTitle)
                                .font(.headline)
                                .foregroundStyle(.primary)
                            if let reportPriorityLabel = message.reportPriorityLabel,
                               !reportPriorityLabel.isEmpty {
                                Text(reportPriorityLabel)
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(.orange)
                            }
                            Text(reportSummary)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .lineLimit(3)
                            Text("\(reportTargetLabel) / \(reportAuthorName)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            if let reportTimeLabel = message.reportTimeLabel {
                                Text(reportTimeLabel)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.blue.opacity(0.08))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.blue.opacity(0.18), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("contact_report_message_\(reportId)")
                } else {
                    Text(message.body)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .foregroundStyle(message.isCurrentUser ? .white : .primary)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(message.isCurrentUser ? Color.accentColor : Color.blue.opacity(0.10))
                        )
                }

                if message.reportId == nil, let timeLabel = message.timeLabel {
                    Text(timeLabel)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            if !message.isCurrentUser {
                Spacer(minLength: 48)
            }
        }
    }
}
