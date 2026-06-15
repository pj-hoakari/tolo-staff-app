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
        List(state.rooms, id: \.id) { room in
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
            }
            .accessibilityIdentifier("contact_chat_room_\(room.id)")
        }
        .accessibilityIdentifier("contact_chat_room_list")
    }

    private var chatDetail: some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    onBackToRooms()
                } label: {
                    Label("contact_chat_back", systemImage: "chevron.left")
                }
                .accessibilityIdentifier("contact_chat_back_button")

                Spacer()
            }
            .padding(.horizontal)
            .padding(.vertical, 8)

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(state.messages, id: \.id) { message in
                        messageBubble(message)
                    }
                }
                .padding()
            }

            Divider()

            HStack(spacing: 10) {
                TextField(
                    String(localized: "contact_chat_message_placeholder"),
                    text: Binding(
                        get: { state.draftText },
                        set: { onDraftChanged($0) }
                    )
                )
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("contact_chat_message_input")

                Button {
                    onSendClicked()
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .buttonStyle(.borderedProminent)
                .disabled(state.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .accessibilityIdentifier("contact_chat_send_button")
            }
            .padding()
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
                    .background(message.isFromCurrentUser ? Color.blue : Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text(message.timeLabel)
                    .font(.caption2)
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
