import SwiftUI
import SharedLogic

struct InstructionsTabRootView: View {
    let state: InstructionsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onInstructionSelected: (String) -> Void = { _ in }
    var onThreadOpened: () -> Void = {}
    var onDetailClosed: () -> Void = {}

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
                        onOpenThread: onThreadOpened
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
            .frame(minHeight: 44, alignment: .center)
            .padding(12)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14))
            .contentShape(Rectangle())
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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                InstructionDetailHeroCard(instruction: instruction)
                InstructionDetailBodyCard(instruction: instruction)

                Button(action: onOpenThread) {
                    Label("スレッドを見る", systemImage: "bubble.left.and.text.bubble.right.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .accessibilityIdentifier("instruction_detail_open_thread_button")
            }
            .padding(20)
        }
        .background(
            LinearGradient(
                colors: [
                    Color(red: 0.96, green: 0.98, blue: 1.0),
                    Color(red: 0.99, green: 0.99, blue: 0.97)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .accessibilityIdentifier("instruction_detail")
    }
}

private struct InstructionDetailHeroCard: View {
    let instruction: InstructionDetailUiModel

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("優先対応")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    Text(instruction.title)
                        .font(.title2)
                        .bold()
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                InstructionBadge(
                    text: instruction.statusLabel,
                    tint: statusTint(for: instruction.statusLabel)
                )
            }

            HStack(spacing: 10) {
                InstructionBadge(
                    text: instruction.target.displayName,
                    tint: .teal
                )

                if !instruction.priorityLabel.isEmpty {
                    InstructionBadge(
                        text: instruction.priorityLabel,
                        tint: .orange
                    )
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 28)
                .fill(.thinMaterial)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 28)
                .strokeBorder(.white.opacity(0.7), lineWidth: 1)
        }
    }
}

private struct InstructionDetailBodyCard: View {
    let instruction: InstructionDetailUiModel

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("内容")
                .font(.headline)

            Text(instruction.body)
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 12) {
                InstructionMetaRow(label: "対象", value: instruction.target.displayName)

                if let locationLabel = instruction.locationLabel {
                    InstructionMetaRow(label: "場所", value: locationLabel)
                }

                if let attachmentSummary = instruction.attachmentSummary {
                    InstructionMetaRow(label: "共有物", value: attachmentSummary)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(.background)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(.quaternary, lineWidth: 1)
        }
    }
}

private struct InstructionBadge: View {
    let text: String
    let tint: Color

    var body: some View {
        Text(text)
            .font(.subheadline)
            .bold()
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(tint.opacity(0.15), in: Capsule())
            .foregroundStyle(tint)
    }
}

private struct InstructionMetaRow: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Text(value)
                .font(.body)
        }
    }
}

private func statusTint(for label: String) -> Color {
    switch label {
    case "対応中":
        return .blue
    case "完了":
        return .green
    case "了解":
        return .teal
    default:
        return .orange
    }
}
