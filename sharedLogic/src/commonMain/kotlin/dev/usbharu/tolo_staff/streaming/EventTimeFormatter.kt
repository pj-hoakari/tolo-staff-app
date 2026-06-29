package dev.usbharu.tolo_staff.streaming

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun formatEventTimeLabel(
    startIso: String?,
    endIso: String?,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val startInput = startIso?.takeIf { it.isNotBlank() }
    val endInput = endIso?.takeIf { it.isNotBlank() }

    if (startInput == null && endInput == null) return ""

    val start = startInput?.parseInstantOrNull()
    val end = endInput?.parseInstantOrNull()

    if ((startInput != null && start == null) || (endInput != null && end == null)) {
        return listOfNotNull(startInput, endInput).joinToString(" - ")
    }

    val startLocal = start?.toLocalDateTime(timeZone)
    val endLocal = end?.toLocalDateTime(timeZone)

    return when {
        startLocal != null && endLocal != null -> {
            val startStr = formatDateTime(startLocal)
            val endStr = if (isSameDay(startLocal, endLocal)) {
                formatTime(endLocal)
            } else {
                formatDateTime(endLocal)
            }
            "$startStr - $endStr"
        }
        startLocal != null -> "${formatDateTime(startLocal)} -"
        endLocal != null -> "- ${formatDateTime(endLocal)}"
        else -> ""
    }
}

private fun formatDateTime(local: LocalDateTime): String =
    "${local.month.number}/${local.day} ${formatTime(local)}"

private fun formatTime(local: LocalDateTime): String =
    "${pad2(local.hour)}:${pad2(local.minute)}"

private fun pad2(value: Int): String = value.toString().padStart(2, '0')

private fun isSameDay(a: LocalDateTime, b: LocalDateTime): Boolean =
    a.year == b.year && a.month.number == b.month.number && a.day == b.day

private fun String.parseInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()
