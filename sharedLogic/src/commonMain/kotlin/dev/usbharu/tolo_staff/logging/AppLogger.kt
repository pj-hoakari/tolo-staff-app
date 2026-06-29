package dev.usbharu.tolo_staff.logging

class AppLogger private constructor(
    private val tag: String,
) {
    inline fun trace(message: () -> String) = log(LogLevel.TRACE, message())

    inline fun debug(message: () -> String) = log(LogLevel.DEBUG, message())

    inline fun info(message: () -> String) = log(LogLevel.INFO, message())

    inline fun warn(message: () -> String) = log(LogLevel.WARN, message())

    inline fun warn(throwable: Throwable, message: () -> String) =
        log(LogLevel.WARN, "${message()} cause=${throwable::class.simpleName}:${throwable.message}")

    inline fun error(throwable: Throwable, message: () -> String) =
        log(LogLevel.ERROR, "${message()} cause=${throwable::class.simpleName}:${throwable.message}")

    fun log(level: LogLevel, message: String) {
        println("[${level.name}][$tag] $message")
    }

    companion object {
        fun withTag(tag: String): AppLogger = AppLogger(tag)
    }
}

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}
