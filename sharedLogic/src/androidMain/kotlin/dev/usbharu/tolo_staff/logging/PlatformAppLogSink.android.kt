package dev.usbharu.tolo_staff.logging

import android.util.Log

internal actual object PlatformAppLogSink : AppLogSink {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        try {
            when (level) {
                LogLevel.TRACE -> if (throwable == null) Log.v(tag, message) else Log.v(tag, message, throwable)
                LogLevel.DEBUG -> if (throwable == null) Log.d(tag, message) else Log.d(tag, message, throwable)
                LogLevel.INFO -> if (throwable == null) Log.i(tag, message) else Log.i(tag, message, throwable)
                LogLevel.WARN -> if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
                LogLevel.ERROR -> if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
            }
        } catch (exception: RuntimeException) {
            // The Android SDK jar used by local host tests contains throwing Log stubs.
            // Never hide a real platform logging failure.
            if (exception.message?.contains("not mocked") != true) {
                throw exception
            }
        }
    }
}
