import SwiftUI

struct FatalErrorDialogView: View {
    let onDismiss: () -> Void

    @AccessibilityFocusState private var isTitleFocused: Bool

    var body: some View {
        ZStack {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 16) {
                Text("error_fatal_title")
                    .font(.headline)
                    .accessibilityFocused($isTitleFocused)

                Text("error_fatal_message")
                    .font(.body)
                    .accessibilityIdentifier("fatal_error_dialog")

                Button("error_confirm", action: onDismiss)
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .accessibilityIdentifier("fatal_error_dismiss_button")
            }
            .padding(20)
            .frame(maxWidth: 360)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            .padding(24)
            .accessibilityElement(children: .contain)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear { isTitleFocused = true }
    }
}
