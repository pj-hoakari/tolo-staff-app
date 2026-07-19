import SnapshotTesting
import SwiftUI
import XCTest
@testable import ToloStaff

@MainActor
final class AppErrorPresentationTests: XCTestCase {
    func testTransientPresentationCarriesStableLocalizationKey() {
        let presentation = TransientErrorPresentation(
            messageKey: "error_grpc_connection",
            eventId: 42
        )

        XCTAssertEqual(presentation.messageKey, "error_grpc_connection")
        XCTAssertEqual(presentation.eventId, 42)
    }

    func testConnectionToastSnapshot() {
        let view = ErrorToastView(messageKey: "error_grpc_connection")
            .frame(width: 390, height: 160, alignment: .top)
            .background(Color(.systemBackground))
            .environment(\.locale, Locale(identifier: "ja"))

        assertSnapshot(
            of: view,
            as: .image(
                precision: 0.99,
                perceptualPrecision: 0.99,
                layout: .fixed(width: 390, height: 160)
            )
        )
    }

    func testCancelledAutoDismissDoesNotAcknowledgeTransientError() async {
        var dismissalCount = 0
        let task = Task { @MainActor in
            await AppShellScreen.autoDismissTransientError(after: .seconds(60)) {
                dismissalCount += 1
            }
        }

        await Task.yield()
        task.cancel()
        await task.value

        XCTAssertEqual(dismissalCount, 0)
    }

    func testReplacementAutoDismissOnlyAcknowledgesCurrentTransientError() async {
        var dismissedEventIds: [Int64] = []
        let replacedTask = Task { @MainActor in
            await AppShellScreen.autoDismissTransientError(after: .seconds(60)) {
                dismissedEventIds.append(1)
            }
        }

        await Task.yield()
        replacedTask.cancel()

        let currentTask = Task { @MainActor in
            await AppShellScreen.autoDismissTransientError(after: .zero) {
                dismissedEventIds.append(2)
            }
        }

        await replacedTask.value
        await currentTask.value

        XCTAssertEqual(dismissedEventIds, [2])
    }
}
