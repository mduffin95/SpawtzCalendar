package com.mduffin95.spawtzcalendar.calendar

import com.mduffin95.spawtzcalendar.model.Fixture
import com.mduffin95.spawtzcalendar.model.Team
import com.mduffin95.spawtzcalendar.model.TeamCalendar
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.util.RandomUidGenerator
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun createCalendarsForTeams(fixtureStore: FixtureStore, createdInstant: Instant): List<TeamCalendar> {
    val teams = fixtureStore.getTeams()
    val calendarList = mutableListOf<TeamCalendar>()
    val ug = RandomUidGenerator()
    for (team in teams) {
        val fixtures = fixtureStore.getFixtures(team.id)
        val teamCalendar = teamCalendar(team, fixtures, createdInstant, ug::generateUid)
        calendarList.add(teamCalendar)
    }
    return calendarList
}

fun teamCalendar(team: Team, fixtures: List<Fixture>, createdInstant: Instant, uidGenerator: () -> Uid): TeamCalendar {
    val events = fixtures.stream()
        .map { fromFixture(it, createdInstant, uidGenerator) }
        .toList()

    val calendar = Calendar()
        .withProdId("-//Events Calendar//iCal4j 1.0//EN")
        .withDefaults()

    events.forEach { event -> calendar.withComponent(event) }
    val cal = calendar.fluentTarget

    val teamCalendar = TeamCalendar(team, cal)
    return teamCalendar
}

private fun fromFixture(fixture: Fixture, createdInstant: Instant, uidGenerator: () -> Uid): VEvent {
    val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
    val timeZone = registry.getTimeZone("Europe/London")
    val start = ZonedDateTime.ofLocal(fixture.dateTime, timeZone.toZoneId(), ZoneOffset.of("Z"))
    val end = start.plus(fixture.duration)

    val eventName = "${fixture.homeTeam.name} vs ${fixture.awayTeam.name}"

    var meeting: VEvent = VEvent(false)
    meeting = meeting.add(DtStamp(createdInstant))
    meeting = meeting.add(DtStart(start));
    meeting = meeting.add(DtEnd(end));
    meeting = meeting.add(Summary(eventName));
    meeting = meeting
        .withProperty(timeZone.vTimeZone.timeZoneId)
        .withProperty(uidGenerator.invoke())
        .fluentTarget as VEvent
    return meeting
}

fun outputCalendar(teamCalendar: TeamCalendar): String {
    // send the cal reference directly.
    val byteArrayOutputStream = ByteArrayOutputStream()
    val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
    val icsOutputter = CalendarOutputter()
    bufferedOutputStream.use { icsOutputter.output(teamCalendar.calendar, it) }

    return byteArrayOutputStream.toString("UTF-8")
}