package com.mduffin95

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.util.RandomUidGenerator

fun createCalendarsForTeams(store: Store): List<TeamCalendar> {
    val teams = store.getTeams()
    val calendarList = mutableListOf<TeamCalendar>()
    val ug = RandomUidGenerator()
    for (team in teams) {
        val fixtures = store.getFixtures(team.id)
        val teamCalendar = teamCalendar(team, fixtures, ug::generateUid)
        calendarList.add(teamCalendar)
    }
    return calendarList
}

fun teamCalendar(team: Team, fixtures: List<Fixture>, uidGenerator: () -> Uid): TeamCalendar {
    val events = fixtures.stream()
        .map { fromFixture(it, uidGenerator) }
        .toList()

    val calendar = Calendar()
        .withProdId("-//Events Calendar//iCal4j 1.0//EN")
        .withDefaults()

    events.forEach { event -> calendar.withComponent(event) }
    val cal = calendar.fluentTarget

    val teamCalendar = TeamCalendar(team, cal)
    return teamCalendar
}
