import SwiftUI
import SharedLogic

struct ContactsTabRootView: View {
    let state: ContactsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onThreadSelected: (String) -> Void = { _ in }
    var onBackToList: () -> Void = {}
    var onNewThreadStarted: () -> Void = {}
    var onTargetTypeSelected: (ContactTargetType) -> Void = { _ in }
    var onTargetSelected: (String) -> Void = { _ in }
    var onDraftChanged: (String) -> Void = { _ in }
    var onSendClicked: () -> Void = {}

    var body: some View {
        NavigationStack(path: navigationPath) {
            List {
                if state.threads.isEmpty {
                    ContentUnavailableView("連絡はありません", systemImage: "message")
                } else {
                    ForEach(state.threads, id: \.id) { thread in
                        Button { onThreadSelected(thread.id) } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(thread.title).font(.headline).foregroundStyle(.primary)
                                Text(thread.lastMessagePreview).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                            }
                        }
                        .accessibilityIdentifier("contact_thread_\(thread.id)")
                    }
                }
            }
            .accessibilityIdentifier("contact_thread_list")
            .navigationTitle("連絡")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                    CurrentStaffHeaderIconView(currentStaff: currentStaff)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("新規連絡", systemImage: "square.and.pencil", action: onNewThreadStarted)
                        .accessibilityIdentifier("contact_new_thread_button")
                }
            }
            .navigationDestination(for: ContactRoute.self) { route in
                switch route {
                case .target: targetSelection
                case .thread:
                    if let thread = state.selectedThread {
                        ContactThreadDetailView(
                            thread: thread,
                            onDraftChanged: onDraftChanged,
                            onSendClicked: onSendClicked
                        )
                    }
                }
            }
        }
    }

    private enum ContactRoute: Hashable { case target, thread }

    private var navigationPath: Binding<[ContactRoute]> {
        Binding(
            get: {
                if state.selectedThread != nil { return [.thread] }
                if state.isChoosingTargetType { return [.target] }
                return []
            },
            set: { path in if path.isEmpty { onBackToList() } }
        )
    }

    private var targetSelection: some View {
        List {
            Section("宛先") {
                Picker("宛先", selection: targetTypeSelection) {
                    Text("担当場所").tag(ContactTargetType.place)
                    Text("個人").tag(ContactTargetType.user)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .accessibilityIdentifier("contact_target_tabs")

                if selectedTargets.isEmpty {
                    ContentUnavailableView(
                        "選択できる宛先がありません",
                        systemImage: "person.slash"
                    )
                    .accessibilityIdentifier("contact_target_empty")
                } else {
                    ForEach(selectedTargets, id: \.id) { target in
                        Button(target.displayName) { onTargetSelected(target.id) }
                    }
                }
            }
        }
        .navigationTitle("宛先を選択")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("contact_target_selection")
    }

    private var selectedTargetType: ContactTargetType {
        state.selectedTargetType ?? .place
    }

    private var selectedTargets: [ContactTargetUiModel] {
        state.availableTargets.filter { $0.type == selectedTargetType }
    }

    private var targetTypeSelection: Binding<ContactTargetType> {
        Binding(
            get: { selectedTargetType },
            set: onTargetTypeSelected
        )
    }
}

private struct ContactThreadDetailView: View {
    let thread: ContactThreadDetailUiModel
    let onDraftChanged: (String) -> Void
    let onSendClicked: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(thread.messages, id: \.id) { message in
                    HStack {
                        if message.isCurrentUser { Spacer(minLength: 44) }
                        VStack(alignment: message.isCurrentUser ? .trailing : .leading, spacing: 4) {
                            Text(message.senderName).font(.caption).foregroundStyle(.secondary)
                            Text(message.body)
                                .padding(10)
                                .foregroundStyle(message.isCurrentUser ? .white : .primary)
                                .background(message.isCurrentUser ? Color.accentColor : Color.blue.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
                        }
                        if !message.isCurrentUser { Spacer(minLength: 44) }
                    }
                    .accessibilityIdentifier("contact_chat_message_\(message.id)")
                }
            }
            .padding()
        }
        .safeAreaInset(edge: .bottom) {
            HStack {
                TextField(
                    "メッセージ",
                    text: Binding(get: { thread.draftMessage }, set: onDraftChanged),
                    axis: .vertical
                )
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("contact_chat_message_input")
                Button("送信", systemImage: "paperplane.fill", action: onSendClicked)
                    .labelStyle(.iconOnly)
                    .disabled(!thread.canReply || thread.draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("contact_chat_send_button")
            }
            .padding()
            .background(.thinMaterial)
        }
        .navigationTitle(thread.title)
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("contact_thread_detail")
    }
}
