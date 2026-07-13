import XCTest

final class SampleScenarioTests: XCTestCase {
    private let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--uitesting"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    func testFourTabNavigationKeepsNewContractScreens() {
        XCTAssertTrue(element("app_shell_placement_name").waitForExistence(timeout: 8))
        XCTAssertTrue(element("app_shell_home_event_card").waitForExistence(timeout: 3))
        XCTAssertTrue(element("app_shell_home_placement_card").waitForExistence(timeout: 3))
        XCTAssertFalse(element("app_shell_home_placement_map_card").exists)

        let tabs = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabs.count, 4)

        tabs.element(boundBy: 1).tap()
        XCTAssertTrue(element("instruction_list").waitForExistence(timeout: 3))

        tabs.element(boundBy: 2).tap()
        XCTAssertTrue(element("report_type_list").waitForExistence(timeout: 3))

        tabs.element(boundBy: 3).tap()
        XCTAssertTrue(element("contact_thread_list").waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["contact_new_thread_button"].waitForExistence(timeout: 3))
    }

    func testRemovedThreadActionsAreNotExposed() {
        XCTAssertFalse(app.buttons["report_detail_open_thread_button"].exists)
        XCTAssertFalse(app.buttons["instruction_detail_open_thread_button"].exists)
    }

    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }
}
