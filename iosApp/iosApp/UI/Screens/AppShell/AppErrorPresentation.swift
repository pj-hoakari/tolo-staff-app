import Foundation

struct TransientErrorPresentation: Identifiable, Equatable {
    let id = UUID()
    let messageKey: String
    let eventId: Int64
}
