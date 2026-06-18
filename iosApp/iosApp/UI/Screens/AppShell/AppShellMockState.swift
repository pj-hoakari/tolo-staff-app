import SharedLogic

extension AppShellMapState {
    static func mock() -> AppShellMapState {
        AppShellMapState(
            venueName: "東京ビッグサイト",
            latitude: 35.6300,
            longitude: 139.7946,
            latitudeDelta: 0.01,
            longitudeDelta: 0.01
        )
    }
}

extension AppShellHomeOverview {
    static func mock(mapState: AppShellMapState = .mock()) -> AppShellHomeOverview {
        AppShellHomeOverview(
            eventName: "Tolo Staff Demo 2026",
            eventTime: "受付開始 09:00 / 開場 10:00",
            placementName: "物販会場",
            placementDetail: "西ホール 入口ゲート A",
            currentInstruction: "来場者導線を確保し、不明点はリーダーへ連絡してください。",
            mapState: mapState,
            currentInstructionId: "instruction-gate-a",
            unreadContactCount: 2,
            pendingReportLabel: "Aゲートの混雑報告を本部へ送信済み"
        )
    }
}

extension AppShellUiState {
    static func mock(selectedTab: AppTab = AppTab.home) -> AppShellUiState {
        let overview = AppShellHomeOverview.mock()

        return AppShellUiState(
            homeOverview: overview,
            currentPlacementName: overview.placementName,
            currentStaff: CurrentStaffUiModel(
                staffId: "tanaka",
                displayName: "田中",
                roleLabel: "Aゲート担当"
            ),
            availableStaff: [
                CurrentStaffUiModel(staffId: "tanaka", displayName: "田中", roleLabel: "Aゲート担当"),
                CurrentStaffUiModel(staffId: "sato", displayName: "佐藤", roleLabel: "巡回担当")
            ],
            selectedTab: selectedTab,
            instructionsTab: InstructionsTabUiState(
                instructions: [
                    InstructionSummaryUiModel(
                        id: "instruction-gate-a",
                        title: "Aゲート前の列を右側へ誘導",
                        targetName: "Aゲート担当",
                        priorityLabel: "高",
                        statusLabel: "対応中",
                        preview: "来場者導線を確保し、右側へ寄せてください。",
                        unreadCount: 1
                    )
                ],
                selectedInstruction: nil,
                isShowingThread: false
            ),
            reportsTab: ReportsTabUiState(
                reportTypes: [
                    ReportTypeUiModel(id: "queue", title: "混雑報告", detailText: "列の長さ、導線、増員要否を記録します。")
                ],
                availablePlaces: [
                    ContactTargetUiModel(id: "place-gate-a", type: .place, displayName: "Aゲート", subtitle: nil)
                ],
                draft: ReportDraftUiModel(
                    selectedTypeId: nil,
                    templateText: "",
                    comment: "",
                    selectedPlaceId: nil,
                    selectedPlaceName: nil,
                    urgencyLabel: "通常",
                    includesImage: false,
                    includesLocation: false
                ),
                step: .typeSelection,
                submittedThread: nil
            ),
            contactsTab: ContactsTabUiState(
                threads: [
                    ContactThreadSummaryUiModel(
                        id: "contact-gate-a",
                        title: "Aゲート担当",
                        target: ContactTargetUiModel(id: "place-gate-a", type: .place, displayName: "Aゲート担当", subtitle: "現担当 3名"),
                        lastMessagePreview: "了解しました。カラーコーンを追加します。",
                        unreadCount: 2,
                        isFormerAssignment: false
                    )
                ],
                selectedThread: nil,
                availableTargets: [],
                selectedTargetType: nil,
                isChoosingTargetType: false,
                formerAssignments: [],
                shouldReturnToInstructionOnBack: false
            ),
            isLoading: false,
            errorMessage: nil
        )
    }
}
