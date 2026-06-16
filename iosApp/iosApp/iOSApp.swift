import SwiftUI
import FirebaseCore
import SharedLogic

@main
struct iOSApp: App {
    init() {
        if FirebaseApp.app() == nil {
            let options = FirebaseOptions(
                googleAppID: "1:1234567890:ios:0123456789abcdef01234567",
                gcmSenderID: "1234567890"
            )
            options.apiKey = "fake-api-key"
            options.projectID = "tolo-communication-poc"
            options.bundleID = Bundle.main.bundleIdentifier ?? "dev.usbharu.tolostaff.ToloStaff"
            FirebaseApp.configure(options: options)
        }
        KoinInitializer.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
