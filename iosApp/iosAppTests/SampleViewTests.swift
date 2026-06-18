import SwiftUI
import XCTest
import SnapshotTesting
import SharedLogic
@testable import ToloStaff

final class SampleViewTests: XCTestCase {
    func testAppShellContentViewCanBeCreated() {
        let state = AppShellUiState.mock(selectedTab: AppTab.home)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testInstructionsTabCanBeCreated() {
        let state = AppShellUiState.mock(selectedTab: AppTab.instructions)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
        XCTAssertEqual(state.instructionsTab.featuredInstruction?.id, state.homeOverview.currentInstructionId)
        XCTAssertEqual(state.instructionsTab.otherInstructions.count, 1)
    }

    func testReportsTabCanBeCreated() {
        let state = AppShellUiState.mock(selectedTab: AppTab.reports)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testContactsTabCanBeCreated() {
        let state = AppShellUiState.mock(selectedTab: AppTab.contacts)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testAppShellContentSnapshotSmoke() {
        let state = AppShellUiState.mock(selectedTab: AppTab.home)

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
