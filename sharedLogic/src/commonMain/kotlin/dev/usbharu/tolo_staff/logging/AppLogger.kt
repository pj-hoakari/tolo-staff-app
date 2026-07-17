package dev.usbharu.tolo_staff.logging

import kotlinx.coroutines.flow.MutableStateFlow

class AppLogger private constructor(
    private val tag: String,
    private val minimumLevelOverride: LogLevel?,
    private val sink: AppLogSink,
) {
    inline fun trace(message: () -> String) {
        if (isEnabled(LogLevel.TRACE)) log(LogLevel.TRACE, message())
    }

    inline fun debug(message: () -> String) {
        if (isEnabled(LogLevel.DEBUG)) log(LogLevel.DEBUG, message())
    }

    inline fun info(message: () -> String) {
        if (isEnabled(LogLevel.INFO)) log(LogLevel.INFO, message())
    }

    inline fun warn(message: () -> String) {
        if (isEnabled(LogLevel.WARN)) log(LogLevel.WARN, message())
    }

    inline fun warn(throwable: Throwable, message: () -> String) {
        if (isEnabled(LogLevel.WARN)) log(LogLevel.WARN, message(), throwable)
    }

    inline fun error(throwable: Throwable, message: () -> String) {
        if (isEnabled(LogLevel.ERROR)) log(LogLevel.ERROR, message(), throwable)
    }

    fun isEnabled(level: LogLevel): Boolean {
        val minimumLevel = minimumLevelOverride ?: AppLogConfig.minimumLevel
        return level.priority >= minimumLevel.priority
    }

    fun log(level: LogLevel, message: String) {
        log(level, message, null)
    }

    fun log(level: LogLevel, message: String, throwable: Throwable?) {
        if (isEnabled(level)) {
            sink.log(level, tag, message, throwable)
        }
    }

    companion object {
        /**
         * Creates a logger that follows [AppLogConfig]. INFO and higher levels are
         * enabled until application startup configures the debug-build setting.
         */
        fun withTag(tag: String): AppLogger =
            AppLogger(tag, null, PlatformAppLogSink)

        fun withTag(tag: String, minimumLevel: LogLevel): AppLogger =
            AppLogger(tag, minimumLevel, PlatformAppLogSink)

        internal fun withSink(
            tag: String,
            minimumLevel: LogLevel?,
            sink: AppLogSink,
        ): AppLogger = AppLogger(tag, minimumLevel, sink)
    }
}

enum class LogLevel(internal val priority: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
}

/**
 * Process-wide logging policy. Configure this once from the validated runtime config
 * during application startup. The default deliberately excludes diagnostic logging.
 */
object AppLogConfig {
    private val configuredMinimumLevel = MutableStateFlow(LogLevel.INFO)

    val minimumLevel: LogLevel
        get() = configuredMinimumLevel.value

    fun configure(isDebugBuild: Boolean) {
        configuredMinimumLevel.value = if (isDebugBuild) LogLevel.DEBUG else LogLevel.INFO
    }
}

internal fun interface AppLogSink {
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    )
}

internal expect object PlatformAppLogSink : AppLogSink
