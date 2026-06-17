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
            mapState: mapState
        )
    }
}

extension AppShellUiState {
    static func mock(selectedTab: AppTab = AppTab.home) -> AppShellUiState {
        let overview = AppShellHomeOverview.mock()

        return AppShellUiState(
            homeOverview: overview,
            currentPlacementName: overview.placementName,
            selectedTab: selectedTab,
            isLoading: false,
            errorMessage: nil
        )
    }
}
