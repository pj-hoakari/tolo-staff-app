# Repository Guidelines

## Project Structure & Module Organization

This is a Kotlin Multiplatform project named `ToloStaff`.

- `androidApp/` contains the Android application entry point, manifest, and Android resources.
- `iosApp/` contains the Xcode project and SwiftUI app entry point.
- `sharedLogic/src/` contains cross-platform business logic. Put common code in `commonMain/kotlin`, shared tests in `commonTest/kotlin`, and platform-specific code in `androidMain`, `iosMain`, or related test source sets.
- `sharedUI/src/` contains shared Compose Multiplatform UI and Compose resources under `commonMain/composeResources`.
- Gradle configuration is centralized in `settings.gradle.kts`, root `build.gradle.kts`, and `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

- `./gradlew :androidApp:assembleDebug` builds the Android debug APK.
- `./gradlew :sharedLogic:testAndroidHostTest :sharedUI:testAndroidHostTest` runs Android host tests for shared modules.
- `./gradlew :sharedLogic:iosSimulatorArm64Test` runs iOS simulator tests for shared logic.
- `./gradlew build` runs the full Gradle build when all configured targets are available.
- Open `iosApp/` in Xcode to build and run the iOS app.

## Coding Style & Naming Conventions

Use Kotlin conventions with 4-space indentation, `PascalCase` for types and composables, `camelCase` for functions and properties, and package names under `dev.usbharu.tolo_staff`. Keep shared APIs in `commonMain` unless they require platform behavior; then define the common contract and implement it in platform source sets. Swift files should follow standard Swift naming and keep SwiftUI view names in `PascalCase`.

## Testing Guidelines

Tests use `kotlin.test`. Place portable tests in `commonTest`, Android host tests in `androidHostTest`, and iOS-specific tests in `iosTest` or simulator-specific source sets. Name test files after the unit under test, for example `GreetingUtilTest.kt`, and prefer focused tests for shared logic before adding platform-specific coverage.

## Commit & Pull Request Guidelines

This checkout does not include Git history, so no project-specific commit convention could be verified. Use short imperative commit subjects such as `Add shared greeting tests` or `Fix Android launcher resource`. Pull requests should include a concise summary, test commands run, linked issues when applicable, and screenshots or recordings for visible Android, iOS, or shared Compose UI changes.

## Security & Configuration Tips

Do not commit local SDK paths, signing files, tokens, or machine-specific IDE state. Keep dependency versions in `gradle/libs.versions.toml`, and prefer Gradle wrapper commands so contributors use the project-pinned Gradle version.
