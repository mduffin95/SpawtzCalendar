package com.mduffin95

fun main() {
    val inputStringTuesday = getInput(1756);
    val inputStringThursday = getInput(1763);
    val tuesdayLeague = XmlParser().parse(inputStringTuesday)
    val thursdayLeague = XmlParser().parse(inputStringThursday)
    val store = InMemoryStore()
        .add(tuesdayLeague)
        .add(thursdayLeague)
    val calendar = createCalendarsForTeams(store)

    calendar.forEach {
        println(outputCalendar(it))
    }

    println(calendar)
}

