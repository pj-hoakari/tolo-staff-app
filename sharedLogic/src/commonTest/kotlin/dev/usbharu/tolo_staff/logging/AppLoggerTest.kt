package dev.usbharu.tolo_staff.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppLoggerTest {
    @Test
    fun messagesBelowMinimumLevelAreNotEvaluatedOrLogged() {
        val entries = mutableListOf<LogEntry>()
        val logger = AppLogger.withSink("Test", LogLevel.INFO, entries.sink())
        var evaluated = false

        logger.debug {
            evaluated = true
            "diagnostic detail"
        }

        assertFalse(evaluated)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun messagesAtOrAboveMinimumLevelAreLogged() {
        val entries = mutableListOf<LogEntry>()
        val logger = AppLogger.withSink("Test", LogLevel.INFO, entries.sink())

        logger.info { "started" }
        logger.warn { "retrying" }
        logger.error(IllegalStateException("failed")) { "stopped" }

        assertEquals(listOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR), entries.map { it.level })
        assertEquals(listOf("started", "retrying", "stopped"), entries.map { it.message })
    }

    @Test
    fun throwableIsPassedToSinkWithoutBeingFlattenedIntoMessage() {
        val entries = mutableListOf<LogEntry>()
        val logger = AppLogger.withSink("Test", LogLevel.INFO, entries.sink())
        val cause = IllegalArgumentException("root cause")
        val failure = IllegalStateException("operation failed", cause)

        logger.warn(failure) { "Request failed" }

        assertEquals("Request failed", entries.single().message)
        assertSame(failure, entries.single().throwable)
        assertSame(cause, entries.single().throwable?.cause)
    }

    @Test
    fun enabledLevelsFollowConfiguredMinimum() {
        val logger = AppLogger.withSink("Test", LogLevel.WARN) { _, _, _, _ -> }

        assertFalse(logger.isEnabled(LogLevel.INFO))
        assertTrue(logger.isEnabled(LogLevel.WARN))
        assertTrue(logger.isEnabled(LogLevel.ERROR))
    }

    @Test
    fun sharedConfigUpdatesAlreadyCreatedLogger() {
        val entries = mutableListOf<LogEntry>()
        val logger = AppLogger.withSink("Test", minimumLevel = null, entries.sink())

        try {
            AppLogConfig.configure(isDebugBuild = false)
            logger.debug { "release detail" }

            AppLogConfig.configure(isDebugBuild = true)
            logger.debug { "debug detail" }

            assertEquals(listOf("debug detail"), entries.map { it.message })
        } finally {
            AppLogConfig.configure(isDebugBuild = false)
        }
    }

    private fun MutableList<LogEntry>.sink(): AppLogSink =
        AppLogSink { level, tag, message, throwable ->
            add(LogEntry(level, tag, message, throwable))
        }

    private data class LogEntry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
