import SwiftUI

struct ErrorToastView: View {
    let messageKey: String

    var body: some View {
        Label {
            Text(LocalizedStringKey(messageKey))
                .multilineTextAlignment(.leading)
        } icon: {
            Image(systemName: "wifi.exclamationmark")
                .accessibilityHidden(true)
        }
        .font(.body)
        .foregroundStyle(.primary)
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
        .overlay {
            RoundedRectangle(cornerRadius: 14)
                .stroke(.secondary, lineWidth: 1)
        }
        .padding()
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("connection_error_toast")
        .accessibilityAddTraits(.isStaticText)
    }
}
