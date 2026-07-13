import SwiftUI
import SharedLogic

struct InstructionsTabRootView: View {
    let state: InstructionsTabUiState
    let currentStaff: CurrentStaffUiModel
    var onInstructionSelected: (String) -> Void = { _ in }
    var onDetailClosed: () -> Void = {}

    var body: some View {
        NavigationStack(path: navigationPath) {
            List {
                if let featured = state.featuredInstruction {
                    Section("対応中") {
                        instructionButton(featured)
                    }
                }
                Section("その他の指示") {
                    if state.otherInstructions.isEmpty {
                        Text("表示できる指示はありません")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(state.otherInstructions, id: \.id) { instruction in
                            instructionButton(instruction)
                        }
                    }
                }
            }
            .accessibilityIdentifier("instruction_list")
            .navigationTitle("指示")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(id: "current-staff-header", placement: .topBarLeading) {
                    CurrentStaffHeaderIconView(currentStaff: currentStaff)
                }
            }
            .navigationDestination(for: String.self) { _ in
                if let detail = state.selectedInstruction {
                    InstructionDetailView(instruction: detail)
                        .navigationTitle("指示詳細")
                        .navigationBarTitleDisplayMode(.inline)
                }
            }
        }
    }

    private var navigationPath: Binding<[String]> {
        Binding(
            get: { state.selectedInstruction.map { [$0.id] } ?? [] },
            set: { path in if path.isEmpty { onDetailClosed() } }
        )
    }

    private func instructionButton(_ instruction: InstructionSummaryUiModel) -> some View {
        Button {
            onInstructionSelected(instruction.id)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(instruction.title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Spacer()
                    Text(instruction.statusLabel)
                        .font(.caption)
                        .foregroundStyle(.blue)
                }
                Text(instruction.preview)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Text(instruction.targetName)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("instruction_row_\(instruction.id)")
    }
}

private struct InstructionDetailView: View {
    let instruction: InstructionDetailUiModel

    var body: some View {
        List {
            Section("概要") {
                Text(instruction.title).font(.headline)
                Text(instruction.statusLabel).foregroundStyle(.blue)
                Text(instruction.target.displayName).foregroundStyle(.secondary)
            }
            Section("内容") {
                Text(instruction.body)
            }
        }
        .accessibilityIdentifier("instruction_detail_screen")
    }
}
