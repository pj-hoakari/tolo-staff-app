import SwiftUI
import XCTest
import SnapshotTesting
import SharedLogic
@testable import ToloStaff

final class SampleViewTests: XCTestCase {
    func testAppShellContentViewCanBeCreated() {
        let state = AppShellUiState(selectedTab: AppTab.home)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testInstructionsTabCanBeCreated() {
        let state = AppShellUiState(selectedTab: AppTab.instructions)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
        XCTAssertNil(state.instructionsTab.featuredInstruction)
        XCTAssertEqual(state.instructionsTab.otherInstructions.count, 0)
    }

    func testReportsTabCanBeCreated() {
        let state = AppShellUiState(selectedTab: AppTab.reports)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testContactsTabCanBeCreated() {
        let state = AppShellUiState(selectedTab: AppTab.contacts)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testAppShellContentSnapshotSmoke() {
        let state = AppShellUiState(selectedTab: AppTab.home)

        assertSnapshot(
            of: AppShellContentView(state: state).frame(width: 390, height: 844),
            as: .image(layout: .fixed(width: 390, height: 844)),
            record: true
        )
    }

    func testSampleContentViewCanBeCreated() {
        let state = SampleUiState(
            message: "KMP is ready",
            tapCount: 2,
            lastAction: "primary"
        )

        let view = SampleContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testSampleContentSnapshotSmoke() {
        let state = SampleUiState(
            message: "KMP is ready",
            tapCount: 1,
            lastAction: "primary"
        )

        assertSnapshot(
            of: SampleContentView(state: state).frame(width: 390, height: 844),
            as: .image(layout: .fixed(width: 390, height: 844)),
            record: true
        )
    }
}
