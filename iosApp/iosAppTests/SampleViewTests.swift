import SwiftUI
import XCTest
import SnapshotTesting
import SharedLogic
@testable import ToloStaff

final class SampleViewTests: XCTestCase {
    func testAppShellContentViewCanBeCreated() {
        let state = AppShellUiState(
            currentPlacementName: "東京会場",
            selectedTab: AppTab.home
        )

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testAppShellContentSnapshotSmoke() {
        let state = AppShellUiState(
            currentPlacementName: "東京会場",
            selectedTab: AppTab.home
        )

        assertSnapshot(
            of: AppShellContentView(state: state).frame(width: 390, height: 844),
            as: .image(layout: .fixed(width: 390, height: 844)),
            record: true
        )
    }

    func testContactChatContentViewCanBeCreated() {
        let state = ContactChatUiState(
            rooms: [
                ChatRoom(
                    id: "operations",
                    title: "運営本部",
                    lastMessage: "巡回前に配置表を確認してください。",
                    unreadCount: 2
                )
            ],
            selectedRoomId: nil,
            selectedRoomTitle: nil,
            messages: [],
            draftText: ""
        )

        let view = ContactChatContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testContactChatContentSnapshotSmoke() {
        let state = ContactChatUiState(
            rooms: [],
            selectedRoomId: "operations",
            selectedRoomTitle: "運営本部",
            messages: [
                ChatMessage(
                    id: "operations-1",
                    roomId: "operations",
                    senderName: "運営本部",
                    body: "巡回前に配置表を確認してください。",
                    timeLabel: "09:10",
                    isFromCurrentUser: false
                ),
                ChatMessage(
                    id: "sent-operations-1",
                    roomId: "operations",
                    senderName: "あなた",
                    body: "配置につきました",
                    timeLabel: "今",
                    isFromCurrentUser: true
                )
            ],
            draftText: ""
        )

        assertSnapshot(
            of: ContactChatContentView(state: state).frame(width: 390, height: 844),
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
