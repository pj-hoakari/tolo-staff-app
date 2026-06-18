import XCTest

final class SampleScenarioTests: XCTestCase {
    private let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--uitesting"]
        app.launch()
    }

    func testTabNavigationShowsPlacementAndSelectedContent() throws {
        let placement = app.descendants(matching: .any)["app_shell_placement_name"]
        XCTAssertTrue(placement.waitForExistence(timeout: 5))

        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_event_card"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_placement_map_card"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_instruction_card"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_quick_actions"].waitForExistence(timeout: 3))

        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 1).tap()
        XCTAssertTrue(app.descendants(matching: .any)["featured_instruction_card"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["instruction_row_instruction-patrol"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["instruction_other_list"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 2).tap()
        XCTAssertTrue(app.descendants(matching: .any)["report_type_queue"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 3).tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_list"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 0).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_event_card"].waitForExistence(timeout: 3))
    }
}
