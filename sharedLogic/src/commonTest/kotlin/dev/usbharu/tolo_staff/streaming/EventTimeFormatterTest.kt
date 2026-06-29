package dev.usbharu.tolo_staff.streaming

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class EventTimeFormatterTest {
    private val utc = TimeZone.UTC

    @Test
    fun `both null returns empty string`() {
        assertEquals("", formatEventTimeLabel(null, null, utc))
    }

    @Test
    fun `both blank returns empty string`() {
        assertEquals("", formatEventTimeLabel("   ", "", utc))
    }

    @Test
    fun `same day collapses end date to time only`() {
        val start = "2026-06-22T10:00:00Z"
        val end = "2026-06-22T18:00:00Z"
        assertEquals("6/22 10:00 - 18:00", formatEventTimeLabel(start, end, utc))
    }

    @Test
    fun `cross day keeps date on both sides`() {
        val start = "2026-06-22T10:00:00Z"
        val end = "2026-06-23T12:00:00Z"
        assertEquals("6/22 10:00 - 6/23 12:00", formatEventTimeLabel(start, end, utc))
    }

    @Test
    fun `start only shows trailing dash`() {
        assertEquals("6/22 10:00 -", formatEventTimeLabel("2026-06-22T10:00:00Z", null, utc))
    }

    @Test
    fun `end only shows leading dash`() {
        assertEquals("- 6/22 18:00", formatEventTimeLabel(null, "2026-06-22T18:00:00Z", utc))
    }

    @Test
    fun `minutes are zero padded`() {
        val start = "2026-06-05T09:05:00Z"
        val end = "2026-06-05T09:30:00Z"
        assertEquals("6/5 09:05 - 09:30", formatEventTimeLabel(start, end, utc))
    }

    @Test
    fun `malformed start falls back to raw join`() {
        val start = "not-a-date"
        val end = "2026-06-22T18:00:00Z"
        assertEquals("not-a-date - 2026-06-22T18:00:00Z", formatEventTimeLabel(start, end, utc))
    }

    @Test
    fun `malformed end falls back to raw join`() {
        val start = "2026-06-22T10:00:00Z"
        val end = "not-a-date"
        assertEquals("2026-06-22T10:00:00Z - not-a-date", formatEventTimeLabel(start, end, utc))
    }
}
