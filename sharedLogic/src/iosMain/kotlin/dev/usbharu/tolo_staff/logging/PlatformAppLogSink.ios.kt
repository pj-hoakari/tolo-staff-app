package dev.usbharu.tolo_staff.logging

import platform.Foundation.NSLog

internal actual object PlatformAppLogSink : AppLogSink {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val renderedMessage = buildString {
            append('[')
            append(level.name)
            append("][")
            append(tag)
            append("] ")
            append(message)
            if (throwable != null) {
                append('\n')
                append(throwable.stackTraceToString())
            }
        }
        // Kotlin/Native vararg bridging for NSLog is unsafe here and has caused an
        // EXC_BAD_ACCESS during application startup. Pass only the format parameter,
        // escaping percent signs so log content cannot be interpreted as directives.
        NSLog(renderedMessage.replace("%", "%%"))
    }
}
