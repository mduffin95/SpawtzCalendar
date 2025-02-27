package com.mduffin95

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.RandomUidGenerator
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")

        val inputString = getInput()
        parse(inputString)
            .forEach {
                val str = outputCalendar(it)
                val byteStream = ByteStream.fromString(str)
                runBlocking { putObject(bucketName, "calendar/v1/${it.team.id}.ics", byteStream) }
            }

        return "OK"
    }

    suspend fun putObject(
        bucketName: String,
        objectKey: String,
        calendarString: ByteStream,
    ) {
        val request =
            PutObjectRequest {
                bucket = bucketName
                key = objectKey
                body = calendarString
            }

        S3Client { region = "eu-west-1" }.use { s3 ->
            val response = s3.putObject(request)
            println("Tag information is ${response.eTag}")
        }
    }
}

fun parse(input: String): List<TeamCalendar> {
    val store = XmlParser().parse(input)

    val teams = store.getTeams(1763)
    val calendarList = mutableListOf<TeamCalendar>()
    for (team in teams) {
        val fixtures = store.getFixtures(team.id)
        val events = fixtures.stream()
            .map { fromFixture(it) }
            .toList()

        val calendar = Calendar()
            .withProdId("-//Events Calendar//iCal4j 1.0//EN")
            .withDefaults()

        events.forEach { event -> calendar.withComponent(event) }
        val cal = calendar.fluentTarget

        calendarList.add(TeamCalendar(team, cal))
    }
    return calendarList
}

fun getInput(): String {
    val client = JavaHttpClient()

    val request = Request(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Fixtures")
        .query("LeagueId", "1763")
        .query("SeasonId", "90")

    val response = client(request)

    val input = response.bodyString()
    return input
}

fun outputCalendar(teamCalendar: TeamCalendar): String {
    // send the cal reference directly.
    val byteArrayOutputStream = ByteArrayOutputStream()
    val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
    val icsOutputter = CalendarOutputter()
    bufferedOutputStream.use { icsOutputter.output(teamCalendar.calendar, it) }

    return byteArrayOutputStream.toString("UTF-8")
}

fun fromFixture(fixture: Fixture): VEvent {
    val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
    val tz = registry.getTimeZone("Europe/London").vTimeZone
    val start = fixture.dateTime
    val end = start.plus(fixture.duration)

    val eventName = "${fixture.homeTeam.name} vs ${fixture.awayTeam.name}"
    val ug = RandomUidGenerator()

    //    val start =
    val meeting: VEvent = VEvent(start, end, eventName)
        .withProperty(tz.timeZoneId)
        .withProperty(ug.generateUid())
        .fluentTarget as VEvent
    return meeting
}
