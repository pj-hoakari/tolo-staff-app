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

        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 1).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_instructions_title"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 2).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_reports_title"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 3).tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_chat_room_list"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 0).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_event_card"].waitForExistence(timeout: 3))
    }
}
