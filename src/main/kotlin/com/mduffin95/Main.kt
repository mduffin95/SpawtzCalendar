package com.mduffin95

import getHtml

fun main() {
    val leagues = getLeagues()
    val parseLeagues = XmlParser().parseLeagues(leagues)

    val store = InMemoryStore()
        .add(parseLeagues.toLeagueInfos())
    val tuesdayLeague = store.getSeason(TUESDAY)?.let { getLeague(TUESDAY, it) } ?: getLatestSeasonForLeague(TUESDAY)!!

    val thursdayLeague = store.getSeason(THURSDAY)?.let { getLeague(THURSDAY, it) } ?: getLatestSeasonForLeague(THURSDAY)!!

    store
        .add(tuesdayLeague)
        .add(thursdayLeague)
    val html = getHtml(store.getTeams())

    println(html)
}
