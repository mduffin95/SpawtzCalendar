package com.mduffin95

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import getHtml
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

const val TUESDAY : LeagueId = 1756;
const val THURSDAY : LeagueId = 1763;

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")
        val websiteBucketName = System.getenv("WEBSITE_BUCKET_NAME")

        logger.log("Getting league information")
        val leagues = getLeagues()
        val parseLeagues = XmlParser().parseLeagues(leagues)

        val store = InMemoryStore()
            .add(parseLeagues.toLeagueInfos())
        logger.log("Getting Tuesday league")
        val tuesdayLeague = store.getSeason(TUESDAY)?.let { getLeague(TUESDAY, it) } ?: getLatestSeasonForLeague(TUESDAY)!!
        logger.log("Getting Thursday league")
        val thursdayLeague = store.getSeason(THURSDAY)?.let { getLeague(THURSDAY, it) } ?: getLatestSeasonForLeague(THURSDAY)!!

        store
            .add(tuesdayLeague)
            .add(thursdayLeague)

        logger.log("Creating calendar")
        createCalendarsForTeams(store, Instant.now())
            .forEach {
                val str = outputCalendar(it)
                val byteStream = ByteStream.fromString(str)
                runBlocking { putObject(bucketName, "calendar/v1/${it.team.id}.ics", byteStream) }
            }

        logger.log("Storing index.html")
        // store index.html
        val html = getHtml(store.getTeams())
        runBlocking { putObject(websiteBucketName, "index.html", ByteStream.fromString(html), "text/html") }

        logger.log("Done!")
        return "OK"
    }

    suspend fun putObject(
        bucketName: String,
        objectKey: String,
        calendarString: ByteStream,
        contentTypeString: String? = null
    ) {
        val request =
            PutObjectRequest {
                bucket = bucketName
                key = objectKey
                body = calendarString
                contentType = contentTypeString
            }

        S3Client { region = "eu-west-1" }.use { s3 ->
            val response = s3.putObject(request)
            println("Tag information is ${response.eTag}")
        }
    }
}

fun getInput(leagueId: LeagueId, seasonId: SeasonId): String {
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

fun fromFixture(fixture: Fixture, createdInstant: Instant, uidGenerator: () -> Uid): VEvent {
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

fun getLatestSeasonForLeague(leagueId: LeagueId): League? {
    for (seasonId in 90..200) {
        val input = getInput(leagueId, seasonId)
        val league = XmlParser().parse(input)
        if (league.fixtures.isNotEmpty()) {
            return league
        }
    }

    return null
}

fun getLeague(leagueId: LeagueId, seasonId: SeasonId): League {
    val input = getInput(leagueId, seasonId)
    return XmlParser().parse(input)
}
