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

        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_home_title"].waitForExistence(timeout: 3))

        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 1).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_instructions_title"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 2).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_reports_title"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 3).tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_chat_room_list"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 0).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_content_home_title"].waitForExistence(timeout: 3))
    }

    func testContactChatRoomSelectionAndMockSend() throws {
        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 3).tap()

        let room = app.descendants(matching: .any)["contact_chat_room_operations"]
        XCTAssertTrue(room.waitForExistence(timeout: 3))
        room.tap()

        let input = app.descendants(matching: .any)["contact_chat_message_input"]
        XCTAssertTrue(input.waitForExistence(timeout: 3))
        input.tap()
        input.typeText("Arrived at post")

        let sendButton = app.descendants(matching: .any)["contact_chat_send_button"]
        XCTAssertTrue(sendButton.waitForExistence(timeout: 3))
        sendButton.tap()

        let sentMessage = app.descendants(matching: .any)["contact_chat_message_sent-operations-1"]
        XCTAssertTrue(sentMessage.waitForExistence(timeout: 3))

        let backButton = app.descendants(matching: .any)["contact_chat_back_button"]
        XCTAssertTrue(backButton.waitForExistence(timeout: 3))
        backButton.tap()

        XCTAssertTrue(app.descendants(matching: .any)["contact_chat_room_list"].waitForExistence(timeout: 3))
    }
}
