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

                HomeTextCard(
                    titleKey: "app_shell_home_instruction_card_title",
                    systemImage: "checklist",
                    primaryText: overview.currentInstruction,
                    secondaryText: nil,
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

#Preview {
    NavigationStack {
        HomeTabContentView(overview: AppShellHomeOverview.mock())
            .navigationTitle("app_shell_home_title")
    }
}
