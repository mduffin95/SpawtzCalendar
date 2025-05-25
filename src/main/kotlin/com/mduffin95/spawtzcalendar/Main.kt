package com.mduffin95.spawtzcalendar

import com.mduffin95.spawtzcalendar.calendar.XmlParser
import com.mduffin95.spawtzcalendar.calendar.getFixtureStore
import com.mduffin95.spawtzcalendar.calendar.getLatestSeasonForLeague
import com.mduffin95.spawtzcalendar.calendar.getLeague
import com.mduffin95.spawtzcalendar.calendar.getLeagues
import getHtml

fun main() {
    val leagues = getLeagues()
    val parseLeagues = XmlParser().parseLeagues(leagues)

    val store = getFixtureStore()
        .add(parseLeagues.toLeagueInfos())
    val tuesdayLeague = store.getSeason(TUESDAY)?.let { getLeague(TUESDAY, it) } ?: getLatestSeasonForLeague(TUESDAY)!!

    val thursdayLeague = store.getSeason(THURSDAY)?.let { getLeague(THURSDAY, it) } ?: getLatestSeasonForLeague(THURSDAY)!!

    store
        .add(tuesdayLeague)
        .add(thursdayLeague)
    val html = getHtml(store.getTeams())

    println(html)
}
