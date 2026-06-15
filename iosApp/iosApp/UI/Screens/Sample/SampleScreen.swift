import SwiftUI
import SharedLogic

struct SampleScreen: View {
    @StateObject private var wrapper: SampleViewModelWrapper

    init(wrapper: SampleViewModelWrapper = SampleViewModelWrapper()) {
        _wrapper = StateObject(wrappedValue: wrapper)
    }

    var body: some View {
        SampleContentView(
            state: wrapper.state,
            onPrimaryAction: { wrapper.onPrimaryActionClicked() }
        )
        .navigationTitle(String(localized: "sample_title"))
    }
}

struct SampleContentView: View {
    let state: SampleUiState
    var onPrimaryAction: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "swift")
                .font(.system(size: 96))
                .foregroundStyle(.tint)
                .accessibilityIdentifier("sample_icon")

            Text(state.message)
                .font(.title2)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("sample_message")

            Text(String(localized: "sample_tap_count \(state.tapCount)"))
                .font(.body)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("sample_tap_count")

            Button(String(localized: "sample_primary_action")) {
                onPrimaryAction()
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("sample_primary_button")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

#Preview {
    NavigationStack {
        SampleContentView(
            state: SampleUiState(
                message: "KMP is ready",
                tapCount: 1,
                lastAction: "primary"
            )
        )
    }
}
