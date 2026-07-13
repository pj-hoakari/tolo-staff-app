import SwiftUI
import SharedLogic

struct HomeTabContentView: View {
    let overview: AppShellHomeOverview
    var errorMessage: String?
    var onOpenInstruction: () -> Void = {}
    var onOpenReport: () -> Void = {}
    var onOpenContacts: () -> Void = {}

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                if let errorMessage {
                    Text(errorMessage)
                        .font(.subheadline)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityIdentifier("app_shell_home_error")
                }

                HomeEventCard(eventName: overview.eventName, eventTime: overview.eventTime)
                    .accessibilityIdentifier("app_shell_home_event_card")

                HomePlacementCard(placementName: overview.placementName)
                    .accessibilityIdentifier("app_shell_home_placement_card")

                AppShellInstructionCard(
                    instruction: overview.currentInstruction.isEmpty ? nil : overview.asInstructionCardModel(),
                    identifier: "app_shell_home_instruction_card",
                    action: onOpenInstruction
                )

                HStack(spacing: 12) {
                    HomeActionButton(title: "報告する", systemImage: "doc.badge.plus", action: onOpenReport)
                    HomeActionButton(title: "連絡を見る", systemImage: "message.badge", action: onOpenContacts)
                }
                .accessibilityIdentifier("app_shell_home_quick_actions")

                if !overview.pendingReportLabel.isEmpty {
                    Text(overview.pendingReportLabel)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }
}

private struct HomeEventCard: View {
    let eventName: String
    let eventTime: String

    var body: some View {
        card {
            Label("本日のイベント", systemImage: "calendar")
                .font(.headline)
                .foregroundStyle(.secondary)
            Text(eventName)
                .font(.title3.bold())
            if !eventTime.isEmpty {
                Label(eventTime, systemImage: "clock.fill")
                    .font(.subheadline)
                    .foregroundStyle(.blue)
            }
        }
    }
}

private struct HomePlacementCard: View {
    let placementName: String

    var body: some View {
        card {
            Label("配置場所", systemImage: "mappin.and.ellipse")
                .font(.headline)
                .foregroundStyle(.secondary)
            Text(placementName)
                .font(.title3.bold())
        }
    }
}

private struct HomeActionButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
        }
        .buttonStyle(.bordered)
    }
}

private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
    VStack(alignment: .leading, spacing: 10, content: content)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
}

struct InstructionCardModel {
    let id: String
    let title: String?
    let bodyText: String
    let targetName: String?
    let statusLabel: String?
}

private extension AppShellHomeOverview {
    func asInstructionCardModel() -> InstructionCardModel {
        InstructionCardModel(
            id: currentInstructionId ?? "home-current-instruction",
            title: currentInstructionTitle,
            bodyText: currentInstruction,
            targetName: currentInstructionTargetName,
            statusLabel: currentInstructionStatusLabel
        )
    }
}

struct AppShellInstructionCard: View {
    let instruction: InstructionCardModel?
    let identifier: String
    let action: (() -> Void)?

    var body: some View {
        Group {
            if let instruction {
                if let action {
                    Button(action: action) { InstructionCardBody(instruction: instruction) }
                        .buttonStyle(.plain)
                } else {
                    InstructionCardBody(instruction: instruction)
                }
            } else {
                card {
                    Text("あなたへの指示").font(.headline).foregroundStyle(.secondary)
                    Text("表示できるような内容はありません").font(.title3.bold())
                }
            }
        }
        .accessibilityIdentifier(identifier)
    }
}

private struct InstructionCardBody: View {
    let instruction: InstructionCardModel

    var body: some View {
        card {
            HStack {
                Label("あなたへの指示", systemImage: "checklist")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                Spacer()
                if let status = instruction.statusLabel, !status.isEmpty {
                    Text(status)
                        .font(.caption)
                        .foregroundStyle(.blue)
                }
            }
            if let title = instruction.title, !title.isEmpty {
                Text(title).font(.title3.bold())
            }
            Text(instruction.bodyText)
            if let target = instruction.targetName, !target.isEmpty {
                Label(target, systemImage: "person.crop.circle")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
}
