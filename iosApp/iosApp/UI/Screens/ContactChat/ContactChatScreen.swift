import SwiftUI
import SharedLogic

struct ContactChatScreen: View {
    @StateObject private var wrapper: ContactChatViewModelWrapper

    init(wrapper: ContactChatViewModelWrapper = ContactChatViewModelWrapper()) {
        _wrapper = StateObject(wrappedValue: wrapper)
    }

    var body: some View {
        ContactChatContentView(
            state: wrapper.state,
            onRoomSelected: { wrapper.onRoomSelected($0) },
            onBackToRooms: { wrapper.onBackToRooms() },
            onDraftChanged: { wrapper.onDraftChanged($0) },
            onSendClicked: { wrapper.onSendClicked() }
        )
        .navigationTitle(wrapper.state.selectedRoomTitle ?? String(localized: "contact_chat_title"))
        .toolbar {
            if wrapper.state.selectedRoomId != nil {
                ToolbarItem(placement: .topBarLeading) {
                    Button("contact_chat_back", systemImage: "chevron.left") {
                        wrapper.onBackToRooms()
                    }
                    .accessibilityIdentifier("contact_chat_back_button")
                }
            }
        }
    }
}

struct ContactChatContentView: View {
    let state: ContactChatUiState
    var onRoomSelected: (String) -> Void = { _ in }
    var onBackToRooms: () -> Void = {}
    var onDraftChanged: (String) -> Void = { _ in }
    var onSendClicked: () -> Void = {}

    var body: some View {
        Group {
            if state.selectedRoomId == nil {
                roomList
            } else {
                chatDetail
            }
        }
    }

    private var roomList: some View {
        List {
            ForEach(state.rooms, id: \.id) { room in
                Button {
                    onRoomSelected(room.id)
                } label: {
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(room.title)
                                .font(.headline)
                                .foregroundStyle(.primary)

                            Text(room.lastMessage)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }

                        Spacer()

                        if room.unreadCount > 0 {
                            Text("\(room.unreadCount)")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .frame(minWidth: 24, minHeight: 24)
                                .background(.blue)
                                .clipShape(Circle())
                        }
                    }
                    .padding(.vertical, 8)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("contact_chat_room_\(room.id)")
            }
        }
        .accessibilityIdentifier("contact_chat_room_list")
    }

    private var chatDetail: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(state.messages, id: \.id) { message in
                    messageBubble(message)
                }
            }
            .padding()
        }
        .safeAreaInset(edge: .bottom) {
            HStack(alignment: .bottom, spacing: 12) {
                TextField(
                    String(localized: "contact_chat_message_placeholder"),
                    text: Binding(
                        get: { state.draftText },
                        set: { onDraftChanged($0) }
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

                Button("Send", systemImage: "paperplane.fill") {
                    onSendClicked()
                }
                .labelStyle(.iconOnly)
                .frame(width: 44, height: 44)
                .background(Color.accentColor, in: Circle())
                .foregroundStyle(.white)
                .disabled(state.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .opacity(state.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.55 : 1)
                .accessibilityIdentifier("contact_chat_send_button")
            }
            .padding(.horizontal)
            .padding(.top, 12)
            .padding(.bottom, 8)
            .background(.thinMaterial)
        }
    }

    private func messageBubble(_ message: ChatMessage) -> some View {
        HStack {
            if message.isFromCurrentUser {
                Spacer(minLength: 44)
            }

            VStack(alignment: message.isFromCurrentUser ? .trailing : .leading, spacing: 4) {
                Text(message.senderName)
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text(message.body)
                    .font(.body)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .foregroundStyle(message.isFromCurrentUser ? .white : .primary)
                    .background(message.isFromCurrentUser ? Color.accentColor : Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                Text(message.timeLabel)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if !message.isFromCurrentUser {
                Spacer(minLength: 44)
            }
        }
        .accessibilityIdentifier("contact_chat_message_\(message.id)")
    }
}

#Preview {
    ContactChatContentView(
        state: ContactChatUiState(
            rooms: [
                ChatRoom(id: "operations", title: "運営本部", lastMessage: "巡回前に配置表を確認してください。", unreadCount: 2)
            ],
            selectedRoomId: nil,
            selectedRoomTitle: nil,
            messages: [],
            draftText: ""
        )
    )
}
