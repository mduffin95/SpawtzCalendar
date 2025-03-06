package com.mduffin95

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.util.RandomUidGenerator
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

const val TUESDAY = 1756;
const val THURSDAY = 1763;

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")

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
        createCalendarsForTeams(store)
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

fun getInput(leagueId: Int, seasonId: Int): String {
    val client = JavaHttpClient()

    // Tuesday =
    val request = Request(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Fixtures")
        .query("LeagueId", leagueId.toString())
        .query("SeasonId", seasonId.toString())

    val response = client(request)

    val input = response.bodyString()
    return input
}

fun getLeagues(): String {
    val client = JavaHttpClient()

    val request = Request(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Leagues")

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

fun fromFixture(fixture: Fixture, uidGenerator: () -> Uid): VEvent {
    val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
    val tz = registry.getTimeZone("Europe/London").vTimeZone
    val start = fixture.dateTime
    val end = start.plus(fixture.duration)

    val eventName = "${fixture.homeTeam.name} vs ${fixture.awayTeam.name}"

    val meeting: VEvent = VEvent(start, end, eventName)
        .withProperty(tz.timeZoneId)
        .withProperty(uidGenerator.invoke())
        .fluentTarget as VEvent
    return meeting
}
