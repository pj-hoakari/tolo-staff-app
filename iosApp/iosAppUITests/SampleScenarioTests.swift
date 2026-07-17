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
        XCTAssertFalse(element("auth_login_button").exists)
        XCTAssertFalse(app.buttons["auth_logout_button"].exists)
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

    func testConnectionFailureToastDoesNotBlockNavigation() {
        app.terminate()
        app.launchArguments = ["--uitesting", "--uitesting-error=grpc"]
        app.launch()

        let toast = element("connection_error_toast")
        XCTAssertTrue(toast.waitForExistence(timeout: 3))

        let tabs = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabs.count, 4)
        tabs.element(boundBy: 1).tap()
        XCTAssertTrue(element("instruction_list").waitForExistence(timeout: 3))
    }

    func testFatalErrorDialogCanBeDismissed() {
        app.terminate()
        app.launchArguments = ["--uitesting", "--uitesting-error=fatal"]
        app.launch()

        let toast = element("connection_error_toast")
        if toast.waitForExistence(timeout: 3) {
            let toastDismissed = expectation(
                for: NSPredicate(format: "exists == false"),
                evaluatedWith: toast
            )
            wait(for: [toastDismissed], timeout: 5)
        }

        let dialog = element("fatal_error_dialog")
        XCTAssertTrue(dialog.waitForExistence(timeout: 3))

        let dismissButton = app.buttons.matching(identifier: "fatal_error_dismiss_button").firstMatch
        XCTAssertTrue(dismissButton.waitForExistence(timeout: 3))
        dismissButton.tap()

        let dismissed = expectation(
            for: NSPredicate(format: "exists == false"),
            evaluatedWith: dialog
        )
        wait(for: [dismissed], timeout: 3)
        XCTAssertTrue(element("app_shell_home_event_card").waitForExistence(timeout: 3))
    }

    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }
}
