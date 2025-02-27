package com.mduffin95

import kotlinx.datetime.toJavaLocalDateTime
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.RandomUidGenerator
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.File
import java.io.FileOutputStream
import java.util.stream.Collectors

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Hello, " + name + "!")

    val client = JavaHttpClient()

    val request = Request(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Fixtures")
        .query("LeagueId", "1763")
        .query("SeasonId", "90")

    val response = client(request)

    val league = XmlParser().parse(response.bodyString())

    val events = league.week.stream()
        .flatMap { it.fixture.stream() }
        .map { fromFixture(it) }
        .collect(Collectors.toList())

    val calendar = Calendar()
        .withProdId("-//Events Calendar//iCal4j 1.0//EN")
        .withDefaults()

    events.forEach { event -> calendar.withComponent(event) }
    val cal = calendar.fluentTarget
//    println(calendar.fluentTarget)

    val file = File("").absoluteFile.toString() + "/tag1.ics"
    val icsOutputStream = FileOutputStream(file)
    val icsOutputter = CalendarOutputter()

    // send the cal reference directly.
    icsOutputter.output(cal, icsOutputStream)

//    println("Done")
//    for (i in 1..5) {
//        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
//        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
//        println("i = $i")
//    }
}

fun fromFixture(fixture: Fixture): VEvent {
    val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
    val tz = registry.getTimeZone("Europe/London").vTimeZone
    val start = fixture.dateTime.toJavaLocalDateTime();
    val end = start.plusMinutes(fixture.duration.toLong())

//    val start = DateTime(Date(fixture.dateTime.date), tz)
//    val end = fixture.dateTime
    val eventName = "${fixture.fixtureName}: ${fixture.homeTeam} vs ${fixture.awayTeam} (${fixture.divisionName})"
    val ug = RandomUidGenerator()

    //    val start =
    val meeting: VEvent = VEvent(start, end, eventName)
        .withProperty(tz.timeZoneId)
        .withProperty(ug.generateUid())
        .fluentTarget as VEvent
    return meeting
}
