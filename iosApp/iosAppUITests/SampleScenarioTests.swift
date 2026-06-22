import XCTest

final class SampleScenarioTests: XCTestCase {
    private let app = XCUIApplication()
    private var screenshotCounter = 0

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
        XCTAssertTrue(app.descendants(matching: .any)["instruction_other_list"].waitForExistence(timeout: 3))
        attachScreenshot(name: "instructions-tab")

        tabBarButtons.element(boundBy: 2).tap()
        XCTAssertTrue(app.descendants(matching: .any)["report_type_queue"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 3).tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_list"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["contact_new_thread_button"].waitForExistence(timeout: 3))

        tabBarButtons.element(boundBy: 0).tap()
        XCTAssertTrue(app.descendants(matching: .any)["app_shell_home_event_card"].waitForExistence(timeout: 3))
    }

    func testReportDetailOpensThreadWhenRelatedReportIsAvailable() throws {
        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 2).tap()
        let relatedReport = app.descendants(matching: .any)["related_report_report-1"]
        try XCTSkipUnless(
            relatedReport.waitForExistence(timeout: 2),
            "関連報告の固定データがない環境ではこのシナリオをスキップする"
        )

        relatedReport.tap()
        XCTAssertTrue(app.descendants(matching: .any)["report_detail_screen"].waitForExistence(timeout: 3))

        let openThreadButton = app.descendants(matching: .any)["report_detail_open_thread_button"]
        XCTAssertTrue(openThreadButton.waitForExistence(timeout: 3))
        openThreadButton.tap()

        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_detail"].waitForExistence(timeout: 3))

        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_list"].waitForExistence(timeout: 3))
    }

    func testInstructionThreadBackReturnsToContactList() throws {
        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 1).tap()
        let instruction = app.descendants(matching: .any)["instruction_row_instruction-2"]
        try XCTSkipUnless(
            instruction.waitForExistence(timeout: 2),
            "指示の固定データがない環境ではこのシナリオをスキップする"
        )

        instruction.tap()
        let openThreadButton = app.buttons["スレッドを見る"]
        XCTAssertTrue(openThreadButton.waitForExistence(timeout: 3))
        openThreadButton.tap()

        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_detail"].waitForExistence(timeout: 3))

        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_list"].waitForExistence(timeout: 3))
    }

    func testReportMessageInContactThreadOpensReportDetailAndBackReturnsToThread() throws {
        let tabBarButtons = app.tabBars.buttons
        XCTAssertGreaterThanOrEqual(tabBarButtons.count, 4)

        tabBarButtons.element(boundBy: 2).tap()
        let relatedReport = app.descendants(matching: .any)["related_report_report-1"]
        try XCTSkipUnless(
            relatedReport.waitForExistence(timeout: 2),
            "関連報告の固定データがない環境ではこのシナリオをスキップする"
        )

        relatedReport.tap()
        let openThreadButton = app.descendants(matching: .any)["report_detail_open_thread_button"]
        XCTAssertTrue(openThreadButton.waitForExistence(timeout: 3))
        openThreadButton.tap()

        let reportMessage = app.descendants(matching: .any)["contact_report_message_report-1"]
        XCTAssertTrue(reportMessage.waitForExistence(timeout: 3))
        reportMessage.tap()
        XCTAssertTrue(app.descendants(matching: .any)["report_detail_screen"].waitForExistence(timeout: 3))

        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.descendants(matching: .any)["contact_thread_detail"].waitForExistence(timeout: 3))
    }

    private func attachScreenshot(name: String) {
        screenshotCounter += 1
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "\(screenshotCounter)-\(name)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
