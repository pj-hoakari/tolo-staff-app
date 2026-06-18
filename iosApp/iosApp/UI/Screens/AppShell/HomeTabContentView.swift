import MapKit
import SwiftUI
import SharedLogic

struct HomeTabContentView: View {
    let overview: AppShellHomeOverview
    var errorMessage: String? = nil
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

                HomeTextCard(
                    titleKey: "app_shell_home_event_card_title",
                    systemImage: "calendar",
                    primaryText: overview.eventName,
                    secondaryText: overview.eventTime
                )
                .accessibilityIdentifier("app_shell_home_event_card")

                HomePlacementMapCard(overview: overview)
                    .accessibilityIdentifier("app_shell_home_placement_map_card")

                HomeInstructionCard(
                    overview: overview,
                    action: onOpenInstruction
                )
                .accessibilityIdentifier("app_shell_home_instruction_card")

                HomeQuickActionsCard(
                    unreadContactCount: Int(overview.unreadContactCount),
                    pendingReportLabel: overview.pendingReportLabel,
                    onOpenReport: onOpenReport,
                    onOpenContacts: onOpenContacts
                )
                .accessibilityIdentifier("app_shell_home_quick_actions")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color(.systemGroupedBackground))
    }
}

private struct HomeInstructionCard: View {
    let overview: AppShellHomeOverview
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            AppleInstructionHeroCard(
                eyebrow: "app_shell_home_instruction_card_title",
                title: overview.currentInstructionTitle,
                bodyText: overview.currentInstruction,
                targetName: overview.currentInstructionTargetName,
                priorityLabel: overview.currentInstructionPriorityLabel,
                statusLabel: overview.currentInstructionStatusLabel,
                locationLabel: overview.currentInstructionLocationLabel,
                attachmentSummary: overview.currentInstructionAttachmentSummary,
                unreadCount: Int(overview.currentInstructionUnreadCount)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct HomeQuickActionsCard: View {
    let unreadContactCount: Int
    let pendingReportLabel: String
    let onOpenReport: () -> Void
    let onOpenContacts: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                quickActionButton(
                    title: "報告する",
                    systemImage: "doc.badge.plus",
                    action: onOpenReport
                )
                quickActionButton(
                    title: unreadContactCount > 0 ? "未読 \(unreadContactCount)" : "連絡を見る",
                    systemImage: "message.badge",
                    action: onOpenContacts
                )
            }

            Text(pendingReportLabel)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
    }

    private func quickActionButton(
        title: String,
        systemImage: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.title3)
                Text(title)
                    .font(.caption)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.bordered)
    }
}

private struct HomeTextCard: View {
    let titleKey: LocalizedStringKey
    let systemImage: String
    let primaryText: String
    let secondaryText: String?
    var action: (() -> Void)? = nil

    var body: some View {
        Group {
            if let action {
                Button(action: action) {
                    content
                }
                .buttonStyle(.plain)
            } else {
                content
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(titleKey, systemImage: systemImage)
                .font(.headline)
                .foregroundStyle(.secondary)

            Text(primaryText)
                .font(.title3)
                .bold()
                .foregroundStyle(.primary)

            if let secondaryText {
                Text(secondaryText)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct HomePlacementMapCard: View {
    let overview: AppShellHomeOverview

    private var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(
            latitude: overview.mapState.latitude,
            longitude: overview.mapState.longitude
        )
    }

    private var region: MKCoordinateRegion {
        MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(
                latitudeDelta: overview.mapState.latitudeDelta,
                longitudeDelta: overview.mapState.longitudeDelta
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("app_shell_home_placement_card_title", systemImage: "mappin.and.ellipse")
                .font(.headline)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 8) {
                Text(overview.placementName)
                    .font(.title3)
                    .bold()
                    .foregroundStyle(.primary)

                Text(overview.placementDetail)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }

            Map(initialPosition: .region(region)) {
                Marker(overview.mapState.venueName, coordinate: coordinate)
            }
            .frame(minHeight: 180)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .allowsHitTesting(false)

            Text(overview.mapState.venueName)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
    }
}

struct AppleInstructionHeroCard: View {
    let eyebrow: LocalizedStringKey
    let title: String?
    let bodyText: String
    let targetName: String?
    let priorityLabel: String?
    let statusLabel: String?
    let locationLabel: String?
    let attachmentSummary: String?
    let unreadCount: Int

    var bodyView: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                Label(eyebrow, systemImage: "checklist")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 8)
                HStack(spacing: 8) {
                    if let priorityLabel = optionalText(priorityLabel) {
                        InstructionStatusPill(
                            text: priorityLabel,
                            tint: priorityLabel == "高" ? .orange : .secondary
                        )
                    }
                    if let statusLabel = optionalText(statusLabel) {
                        InstructionStatusPill(
                            text: statusLabel,
                            tint: statusLabel == "対応中" ? .blue : statusLabel == "完了" ? .green : .secondary
                        )
                    }
                }
            }

            if let title = optionalText(title) {
                Text(title)
                    .font(.title3)
                    .bold()
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Text(bodyText)
                .font(.body)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            InstructionMetaSummaryView(
                targetName: targetName,
                locationLabel: locationLabel,
                attachmentSummary: attachmentSummary,
                unreadCount: unreadCount
            )
        }
    }

    var body: some View {
        bodyView
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Color(.secondarySystemGroupedBackground),
                in: RoundedRectangle(cornerRadius: 16)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(Color.primary.opacity(0.05), lineWidth: 1)
            }
    }
}

struct InstructionMetaSummaryView: View {
    let targetName: String?
    let locationLabel: String?
    let attachmentSummary: String?
    let unreadCount: Int

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
            if let targetName = optionalText(targetName) {
                    InstructionInfoPill(text: targetName, systemImage: "person.crop.circle")
            }
                if let location = optionalText(locationLabel) {
                    InstructionInfoPill(text: location, systemImage: "mappin.circle")
                }
                if let attachment = optionalText(attachmentSummary) {
                    InstructionInfoPill(text: attachment, systemImage: "paperclip")
                }
                if unreadCount > 0 {
                    InstructionInfoPill(text: "\(unreadCount) 件", systemImage: "envelope.badge", tint: .red)
                }
            }
        }
    }
}

private func optionalText(_ text: String?) -> String? {
    guard let text, !text.isEmpty else { return nil }
    return text
}

struct InstructionStatusPill: View {
    let text: String
    let tint: Color

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(tint)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(tint.opacity(0.12), in: Capsule())
    }
}

struct InstructionInfoPill: View {
    let text: String
    let systemImage: String
    var tint: Color = .secondary

    var body: some View {
        Label(text, systemImage: systemImage)
            .font(.subheadline)
            .foregroundStyle(tint)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(.systemBackground), in: Capsule())
            .overlay {
                Capsule()
                    .strokeBorder(Color.primary.opacity(0.06), lineWidth: 1)
            }
    }
}
