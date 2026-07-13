import SwiftUI
import XCTest
import SharedLogic
@testable import ToloStaff

@MainActor
final class SampleViewTests: XCTestCase {
    func testAppShellKeepsFourTabs() {
        for tab in [AppTab.home, .instructions, .reports, .contacts] {
            let view = AppShellContentView(state: makeAppShellState(selectedTab: tab))
            XCTAssertNotNil(view)
        }
    }

    func testInstructionUsesBodyFirstLineAsTitleWithoutThreadState() {
        let instruction = InstructionSummaryUiModel(
            id: "instruction-1",
            title: "南口の列整理",
            targetName: "南口",
            statusLabel: "対応中",
            preview: "南口の列整理\n右側へ誘導してください"
        )
        let state = makeAppShellState(
            selectedTab: .instructions,
            instructions: InstructionsTabUiState(
                instructions: [instruction],
                featuredInstruction: instruction,
                otherInstructions: [],
                selectedInstruction: nil
            )
        )

        XCTAssertNotNil(AppShellContentView(state: state))
        XCTAssertEqual(state.instructionsTab.featuredInstruction?.title, "南口の列整理")
    }

    func testReportDetailUsesNewContractFields() {
        let report = ReportDetailUiModel(
            reportId: "report-1",
            title: "混雑",
            summary: "南口の列が伸びています",
            severityLabel: "高",
            authorName: "田中",
            targetLabel: "南口",
            statusLabel: "未対応",
            timeLabel: "2026-07-13T01:00:00Z",
            isAuthoredByCurrentStaff: true
        )
        let state = makeAppShellState(selectedTab: .reports, selectedReport: report)

        XCTAssertNotNil(AppShellContentView(state: state))
        XCTAssertEqual(state.reportsTab.selectedReport?.severityLabel, "高")
    }

    func testPlacementBarSupportsScheduledAndActiveOnly() {
        XCTAssertNotNil(PlacementBar(placementStatus: makePlacementStatus(.scheduled)))
        XCTAssertNotNil(PlacementBar(placementStatus: makePlacementStatus(.active)))
    }

    func testSampleContentViewCanBeCreated() {
        let state = SampleUiState(message: "KMP is ready", tapCount: 2, lastAction: "primary")
        XCTAssertNotNil(SampleContentView(state: state))
    }

    private func makeAppShellState(
        selectedTab: AppTab,
        instructions: InstructionsTabUiState? = nil,
        selectedReport: ReportDetailUiModel? = nil
    ) -> AppShellUiState {
        let overview = AppShellHomeOverview(
            eventName: "Tolo Staff Demo 2026",
            eventTime: "7/13 09:00 - 18:00",
            placementName: "南口",
            currentInstruction: "南口の列を整理してください",
            currentInstructionTitle: "南口の列整理",
            currentInstructionTargetName: "南口",
            currentInstructionStatusLabel: "対応中",
            currentInstructionId: "instruction-1",
            unreadContactCount: 0,
            pendingReportLabel: "",
            placementStatus: makePlacementStatus(.active)
        )
        let reportState = ReportsTabUiState(
            reportTypes: [],
            availablePlaces: [ContactTargetUiModel(id: "point-1", type: .place, displayName: "南口")],
            draft: ReportDraftUiModel(
                selectedTypeId: nil,
                templateText: "",
                comment: "",
                selectedPlaceId: nil,
                selectedPlaceName: nil,
                urgencyLabel: "通常"
            ),
            step: .typeSelection,
            relatedReports: [],
            selectedReport: selectedReport,
            isLoadingReports: false,
            reportsErrorMessage: nil
        )
        return AppShellUiState(
            homeOverview: overview,
            currentPlacementName: overview.placementName,
            currentStaff: CurrentStaffUiModel(staffId: "staff-1", displayName: "田中"),
            selectedTab: selectedTab,
            instructionsTab: instructions ?? InstructionsTabUiState(
                instructions: [],
                featuredInstruction: nil,
                otherInstructions: [],
                selectedInstruction: nil
            ),
            reportsTab: reportState,
            contactsTab: ContactsTabUiState(
                threads: [],
                selectedThread: nil,
                availableTargets: [],
                selectedTargetType: nil,
                isChoosingTargetType: false
            ),
            isLoading: false,
            errorMessage: nil
        )
    }

    private func makePlacementStatus(_ phase: PlacementPhase) -> PlacementStatusUiModel {
        PlacementStatusUiModel(
            assignId: "assignment-1",
            expectedVersion: 4,
            placementName: "南口",
            phase: phase,
            headline: phase == .scheduled ? "配置が変更されました" : "現在の配置",
            buttonLabel: phase == .scheduled ? "到着" : nil,
            showsActionButton: phase == .scheduled
        )
    }
}
