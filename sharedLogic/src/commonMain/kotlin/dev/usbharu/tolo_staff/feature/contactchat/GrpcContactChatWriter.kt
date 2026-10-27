package dev.usbharu.tolo_staff.feature.contactchat

import dev.usbharu.tolo_staff.logging.AppLogger

/**
 * Direct (thread-scoped) messaging is not part of the current operation API.
 * Staff communication is exposed as event-wide `tolo.operation.v1.StaffMessage`s
 * instead, so this writer is intentionally disabled until the UI supports it.
 */
class GrpcContactChatWriter {
    private val logger = AppLogger.withTag("GrpcContactChatWriter")

    suspend fun sendSimpleMessage(roomId: String, currentStaffId: String, text: String) {
        logger.debug {
            "Direct chat messaging is not supported by the operation API: roomId=$roomId, currentStaffId=$currentStaffId, textLength=${text.length}"
        }
        throw UnsupportedOperationException("Direct messaging is not available")
    }
}
