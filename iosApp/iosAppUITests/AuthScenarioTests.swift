import XCTest

final class AuthScenarioTests: XCTestCase {
    private let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--uitesting-auth-flow"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    func testLoginExchangeAppShellAndLogout() {
        let login = element("auth_login_button")
        XCTAssertTrue(login.waitForExistence(timeout: 8))
        login.tap()

        XCTAssertTrue(element("app_shell_placement_name").waitForExistence(timeout: 8))
        let logout = app.buttons["auth_logout_button"]
        XCTAssertTrue(logout.waitForExistence(timeout: 3))
        logout.tap()

        XCTAssertTrue(element("auth_login_button").waitForExistence(timeout: 3))
    }

    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }
}
