import SwiftUI
import SharedLogic

struct InstructionsTabRootView: View {
    let state: InstructionsTabUiState
    var onInstructionSelected: (String) -> Void = { _ in }
    var onThreadOpened: () -> Void = {}
    var onDetailClosed: () -> Void = {}
    var onThreadClosed: () -> Void = {}
    var onStatusUpdated: (InstructionProgressStatus) -> Void = { _ in }

    var body: some View {
        NavigationStack {
            InstructionListView(
                instructions: state.instructions,
                onInstructionSelected: onInstructionSelected
            )
            .navigationTitle("app_shell_instructions_title")
            .navigationDestination(
                isPresented: Binding(
                    get: { state.selectedInstruction != nil },
                    set: { isPresented in
                        if !isPresented {
                            onDetailClosed()
                        }
                    }
                )
            ) {
                if let selectedInstruction = state.selectedInstruction {
                    InstructionDetailView(
                        instruction: selectedInstruction,
                        onOpenThread: onThreadOpened,
                        onStatusUpdated: onStatusUpdated
                    )
                    .navigationTitle("指示詳細")
                    .navigationBarTitleDisplayMode(.inline)
                    .navigationDestination(
                        isPresented: Binding(
                            get: { state.isShowingThread },
                            set: { isPresented in
                                if !isPresented {
                                    onThreadClosed()
                                }
                            }
                        )
                    ) {
                        InstructionThreadView(instruction: selectedInstruction)
                            .navigationTitle("指示スレッド")
                            .navigationBarTitleDisplayMode(.inline)
                    }
                }
            }
        }
    }
}

private struct InstructionListView: View {
    let instructions: [InstructionSummaryUiModel]
    let onInstructionSelected: (String) -> Void

    var body: some View {
        List(instructions, id: \.id) { instruction in
            Button {
                onInstructionSelected(instruction.id)
            } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(instruction.title)
                            .font(.headline)
                            .foregroundStyle(.primary)
                        Spacer()
                        Text(instruction.priorityLabel)
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }
                    Text(instruction.targetName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(instruction.preview)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                    HStack {
                        Text(instruction.statusLabel)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(.blue.opacity(0.1), in: Capsule())
                        if instruction.unreadCount > 0 {
                            Text("未読 \(instruction.unreadCount)")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                    }
                }
                .padding(.vertical, 6)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("instruction_row_\(instruction.id)")
        }
    }
}

private struct InstructionDetailView: View {
    let instruction: InstructionDetailUiModel
    let onOpenThread: () -> Void
    let onStatusUpdated: (InstructionProgressStatus) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(instruction.title)
                        .font(.title2)
                        .fontWeight(.bold)
                    Text(instruction.body)
                    Text("対象: \(instruction.target.displayName)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("優先度: \(instruction.priorityLabel) / 現在状態: \(instruction.statusLabel)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let locationLabel = instruction.locationLabel {
                        Text("場所: \(locationLabel)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    if let attachmentSummary = instruction.attachmentSummary {
                        Text(attachmentSummary)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("担当者状態")
                        .font(.headline)
                    ForEach(instruction.participants, id: \.staffName) { participant in
                        HStack {
                            Text(participant.staffName)
                            if participant.isFormerStaff {
                                Text("旧担当")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(participant.statusLabel)
                                .font(.caption)
                                .foregroundStyle(participant.isCurrentStaff ? .blue : .secondary)
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("状態を更新")
                        .font(.headline)
                    HStack {
                        statusButton(title: "未確認", status: .unconfirmed)
                        statusButton(title: "了解", status: .acknowledged)
                    }
                    HStack {
                        statusButton(title: "対応中", status: .inProgress)
                        statusButton(title: "完了", status: .completed)
                    }
                }

                Button("スレッドを見る", action: onOpenThread)
                    .buttonStyle(.borderedProminent)
            }
            .padding(16)
        }
        .accessibilityIdentifier("instruction_detail")
    }

    private func statusButton(title: String, status: InstructionProgressStatus) -> some View {
        Button(title) {
            onStatusUpdated(status)
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity)
    }
}

private struct InstructionThreadView: View {
    let instruction: InstructionDetailUiModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(instruction.thread, id: \.id) { message in
                    ThreadMessageBubble(message: message)
                }
            }
            .padding()
        }
        .accessibilityIdentifier("instruction_thread")
    }
}
