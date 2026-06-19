import SwiftUI
import SharedLogic

struct InstructionsTabRootView: View {
    let state: InstructionsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onInstructionSelected: (String) -> Void = { _ in }
    var onThreadOpened: () -> Void = {}
    var onDetailClosed: () -> Void = {}
    var onStatusUpdated: (InstructionProgressStatus) -> Void = { _ in }

    var body: some View {
        NavigationStack {
            InstructionListView(
                featuredInstruction: state.featuredInstruction,
                otherInstructions: state.otherInstructions,
                onInstructionSelected: onInstructionSelected
            )
            .navigationTitle("app_shell_instructions_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                    CurrentStaffHeaderIconView(currentStaff: currentStaff)
                }
            }
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
                }
            }
        }
    }
}

private struct InstructionListView: View {
    let featuredInstruction: InstructionSummaryUiModel?
    let otherInstructions: [InstructionSummaryUiModel]
    let onInstructionSelected: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let featuredInstruction {
                    AppShellInstructionCard(
                        instruction: featuredInstruction.asInstructionCardModel(),
                        identifier: "featured_instruction_card",
                        action: { onInstructionSelected(featuredInstruction.id) }
                    )
                        .padding(.horizontal, 16)
                } else if otherInstructions.isEmpty {
                    AppShellInstructionCard(
                        instruction: nil,
                        identifier: "instruction_empty_state",
                        action: nil
                    )
                        .padding(.horizontal, 16)
                }

                if !otherInstructions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("その他の指示")
                            .font(.headline)
                            .foregroundStyle(.secondary)

                        VStack(spacing: 10) {
                            ForEach(otherInstructions, id: \.id) { instruction in
                                compactInstructionRow(instruction)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .accessibilityIdentifier("instruction_other_list")
                }
            }
            .padding(.vertical, 16)
        }
        .background(Color(.systemGroupedBackground))
    }

    private func compactInstructionRow(_ instruction: InstructionSummaryUiModel) -> some View {
        Button {
            onInstructionSelected(instruction.id)
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(instruction.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.primary)
                    Spacer()
                    Text(instruction.priorityLabel)
                        .font(.caption2)
                        .foregroundStyle(.orange)
                }

                HStack(spacing: 8) {
                    Text(instruction.targetName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(instruction.statusLabel)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if instruction.unreadCount > 0 {
                        Text("未読 \(instruction.unreadCount)")
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("instruction_row_\(instruction.id)")
    }
}

private extension InstructionSummaryUiModel {
    func asInstructionCardModel() -> InstructionCardModel {
        InstructionCardModel(
            id: id,
            title: title,
            bodyText: preview,
            targetName: targetName,
            priorityLabel: priorityLabel,
            statusLabel: statusLabel,
            locationLabel: locationLabel,
            attachmentSummary: attachmentSummary,
            unreadCount: Int(unreadCount)
        )
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
