package com.mduffin95

fun main() {
    val leagues = getLeagues()
    val parseLeagues = XmlParser().parseLeagues(leagues)

    val store = InMemoryStore()
        .add(parseLeagues.toLeagueInfos())
    val tuesdaySeason = store.getSeason(TUESDAY)!!
    val inputStringTuesday = getInput(TUESDAY, tuesdaySeason);
    val tuesdayLeague = XmlParser().parse(inputStringTuesday)

    val thursdaySeason = store.getSeason(THURSDAY)!!
    val inputStringThursday = getInput(THURSDAY, thursdaySeason);
    val thursdayLeague = XmlParser().parse(inputStringThursday)

    store
        .add(tuesdayLeague)
        .add(thursdayLeague)
    val calendar = createCalendarsForTeams(store)

    calendar.forEach {
        println(outputCalendar(it))
    }

    println(calendar)
}

