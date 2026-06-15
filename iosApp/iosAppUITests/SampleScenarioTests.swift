import XCTest

final class SampleScenarioTests: XCTestCase {
    private let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--uitesting"]
        app.launch()
    }

    func testTapPrimaryActionUpdatesCount() throws {
        let button = app.descendants(matching: .any)["sample_primary_button"]
        XCTAssertTrue(button.waitForExistence(timeout: 5))

        button.tap()

        let tapCount = app.descendants(matching: .any)["sample_tap_count"]
        XCTAssertTrue(tapCount.waitForExistence(timeout: 3))
    }
}
