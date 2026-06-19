import SwiftUI
import XCTest
import SnapshotTesting
import SharedLogic
@testable import ToloStaff

final class SampleViewTests: XCTestCase {
    func testAppShellContentViewCanBeCreated() {
        let state = makeAppShellState(selectedTab: AppTab.home)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testInstructionsTabCanBeCreated() {
        let featuredInstruction = InstructionSummaryUiModel(
            id: "instruction-gate-a",
            title: "Aゲート前の列を右側へ誘導",
            targetName: "Aゲート担当",
            priorityLabel: "高",
            statusLabel: "対応中",
            preview: "来場者導線を確保し、右側へ寄せてください。",
            locationLabel: nil,
            attachmentSummary: nil,
            unreadCount: 1
        )
        let state = makeAppShellState(
            selectedTab: AppTab.instructions,
            homeOverview: AppShellHomeOverview(
                eventName: "Tolo Staff Demo 2026",
                eventTime: "Firestore Streaming Demo",
                placementName: "Gate A",
                placementDetail: "North entrance",
                currentInstruction: featuredInstruction.preview,
                currentInstructionTitle: featuredInstruction.title,
                currentInstructionTargetName: featuredInstruction.targetName,
                currentInstructionPriorityLabel: featuredInstruction.priorityLabel,
                currentInstructionStatusLabel: featuredInstruction.statusLabel,
                currentInstructionLocationLabel: nil,
                currentInstructionAttachmentSummary: nil,
                currentInstructionUnreadCount: Int32(featuredInstruction.unreadCount),
                mapState: AppShellMapState(
                    venueName: "Gate A",
                    latitude: 35.0,
                    longitude: 139.0,
                    latitudeDelta: 0.01,
                    longitudeDelta: 0.01
                ),
                currentInstructionId: featuredInstruction.id,
                unreadContactCount: 0,
                pendingReportLabel: ""
            ),
            instructionsTab: InstructionsTabUiState(
                instructions: [featuredInstruction],
                featuredInstruction: featuredInstruction,
                otherInstructions: [],
                selectedInstruction: nil,
                isShowingThread: false
            )
        )

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
        XCTAssertEqual(state.instructionsTab.featuredInstruction?.id, "instruction-gate-a")
        XCTAssertEqual(state.instructionsTab.otherInstructions.count, 0)
    }

    func testReportsTabCanBeCreated() {
        let state = makeAppShellState(selectedTab: AppTab.reports)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func testReportsDetailCanBeCreated() {
        let report = ReportDetailUiModel(
            reportId: "report-1",
            threadId: "report-thread-1",
            title: "導線報告",
            summary: "南口の入場列は安定しています",
            priorityLabel: "通常",
            authorName: "田中",
            targetLabel: "南口",
            timeLabel: "2026-06-19T09:00:00Z",
            isAuthoredByCurrentStaff: true,
            detailPlaceholderMessage: "詳細情報は今後の API 連携で表示予定です。現在は概要のみ確認できます。"
        )
        let state = makeAppShellState(
            selectedTab: AppTab.reports,
            selectedReport: report
        )

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
        XCTAssertEqual(state.reportsTab.selectedReport?.reportId, "report-1")
        XCTAssertEqual(state.reportsTab.selectedReport?.threadId, "report-thread-1")
    }

    func testContactsTabCanBeCreated() {
        let state = makeAppShellState(selectedTab: AppTab.contacts)

        let view = AppShellContentView(state: state)

        XCTAssertNotNil(view)
    }

    func disabled_testAppShellContentSnapshotSmoke() {
        let state = makeAppShellState(selectedTab: AppTab.home)

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

    private func makeAppShellState(
        selectedTab: AppTab,
        homeOverview: AppShellHomeOverview? = nil,
        instructionsTab: InstructionsTabUiState? = nil,
        selectedReport: ReportDetailUiModel? = nil
    ) -> AppShellUiState {
        let overview = homeOverview ?? AppShellHomeOverview(
            eventName: "Tolo Staff Demo 2026",
            eventTime: "Firestore Streaming Demo",
            placementName: "Gate A",
            placementDetail: "North entrance",
            currentInstruction: "Shift update: Move barricades",
            currentInstructionTitle: "Shift update",
            currentInstructionTargetName: "Gate A",
            currentInstructionPriorityLabel: "高",
            currentInstructionStatusLabel: "対応中",
            currentInstructionLocationLabel: nil,
            currentInstructionAttachmentSummary: nil,
            currentInstructionUnreadCount: 1,
            mapState: AppShellMapState(
                venueName: "Gate A",
                latitude: 35.0,
                longitude: 139.0,
                latitudeDelta: 0.01,
                longitudeDelta: 0.01
            ),
            currentInstructionId: "instruction-gate-a",
            unreadContactCount: 0,
            pendingReportLabel: ""
        )

        let currentStaff = CurrentStaffUiModel(
            staffId: "tanaka",
            displayName: "田中",
            roleLabel: "Aゲート担当"
        )

        return AppShellUiState(
            homeOverview: overview,
            currentPlacementName: overview.placementName,
            currentStaff: currentStaff,
            availableStaff: [currentStaff],
            selectedTab: selectedTab,
            instructionsTab: instructionsTab ?? InstructionsTabUiState(
                instructions: [],
                featuredInstruction: nil,
                otherInstructions: [],
                selectedInstruction: nil,
                isShowingThread: false
            ),
            reportsTab: ReportsTabUiState(
                reportTypes: [],
                availablePlaces: [],
                draft: ReportDraftUiModel(
                    selectedTypeId: nil,
                    templateText: "",
                    comment: "",
                    selectedPlaceId: nil,
                    selectedPlaceName: nil,
                    urgencyLabel: "",
                    includesImage: false,
                    includesLocation: false
                ),
                step: .typeSelection,
                relatedReports: [],
                selectedReport: selectedReport,
                isLoadingReports: false,
                reportsErrorMessage: nil
            ),
            contactsTab: ContactsTabUiState(
                threads: [],
                selectedThread: nil,
                availableTargets: [],
                selectedTargetType: nil,
                isChoosingTargetType: false,
                formerAssignments: [],
                selectedThreadBackDestination: .none
            ),
            isLoading: false,
            errorMessage: nil
        )
    }
}
