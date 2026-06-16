import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        KoinInitializer.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
