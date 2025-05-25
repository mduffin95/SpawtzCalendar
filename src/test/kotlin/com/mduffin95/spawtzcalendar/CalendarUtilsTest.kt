package com.mduffin95.spawtzcalendar

import com.mduffin95.spawtzcalendar.persistence.outputCalendar
import com.mduffin95.spawtzcalendar.calendar.teamCalendar
import com.mduffin95.spawtzcalendar.model.Fixture
import com.mduffin95.spawtzcalendar.model.Team
import net.fortuna.ical4j.model.property.Uid
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals

class CalendarUtilsTest {

    @Test
    fun `build team calendar with a single fixture`() {
        val team = Team(1, "Foo")
        val team2 = Team(2, "Bar")
        val fixture = Fixture(1, team, team2, LocalDateTime.of(2024, 3, 5, 20, 0), Duration.ofMinutes(45))
        val instant = Instant.parse("2025-01-01T00:00:00.000Z")
        val calendar = teamCalendar(team, listOf(fixture), instant) { Uid("72b03de4-5e38-4bcd-9b28-f3eb4a87556b") }

        val str = outputCalendar(calendar)

        val expected = """
            BEGIN:VCALENDAR
            PRODID:-//Events Calendar//iCal4j 1.0//EN
            CALSCALE:GREGORIAN
            VERSION:2.0
            BEGIN:VEVENT
            DTSTAMP:20250101T000000Z
            DTSTART;TZID=Europe/London:20240305T200000
            DTEND;TZID=Europe/London:20240305T204500
            SUMMARY:Foo vs Bar
            TZID:Europe/London
            UID:72b03de4-5e38-4bcd-9b28-f3eb4a87556b
            END:VEVENT
            END:VCALENDAR
            
        """.trimIndent().replace("\n", "\r\n")
        assertEquals(expected, str)
    }
}
