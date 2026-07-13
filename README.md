This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/sharedUI](./sharedUI/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./sharedUI/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./sharedUI/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./sharedUI/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### OIDC configuration

ToloStaff uses Authorization Code + PKCE in the system authentication UI. Android values can be supplied as Gradle properties:

- `tolo.oidc.issuer`
- `tolo.oidc.clientId`
- `tolo.oidc.redirectUri` and `tolo.oidc.redirectScheme`
- `tolo.oidc.scopes`
- `tolo.oidc.audience`
- `tolo.oidc.resourceBaseUrl`

For Android Debug builds, `tolo.oidc.debugOriginOverride` controls the origin used to reach a locally running IdP. It defaults to `http://10.0.2.2:8080` for the Android emulator. For example, use `-Ptolo.oidc.debugOriginOverride=http://192.168.1.20:8080` on a physical device. The override is applied to discovery retrieval and loopback endpoints returned by discovery; Release builds ignore it.

For iOS, override the matching `TOLO_OIDC_*` values in `iosApp/Configuration/Config.local.xcconfig`. The redirect URI and registered custom scheme must exactly match the public-client registration at the IdP. The client must allow PKCE S256, token endpoint authentication method `none`, and public-client token exchange from the tenant resource to the event resource. No client secret belongs in either app.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
