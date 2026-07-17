package dev.usbharu.tolo_staff.logging

import java.util.logging.Level
import java.util.logging.Logger

internal actual object PlatformAppLogSink : AppLogSink {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val logger = Logger.getLogger(tag)
        val platformLevel = level.toPlatformLevel()
        if (throwable == null) {
            logger.log(platformLevel, message)
        } else {
            logger.log(platformLevel, message, throwable)
        }
    }

    private fun LogLevel.toPlatformLevel(): Level = when (this) {
        LogLevel.TRACE -> Level.FINEST
        LogLevel.DEBUG -> Level.FINE
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARNING
        LogLevel.ERROR -> Level.SEVERE
    }
}
