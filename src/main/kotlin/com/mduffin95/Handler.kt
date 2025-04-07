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
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.ZonedDateTime

const val TUESDAY = 1756;
const val THURSDAY = 1763;

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")
        val websiteBucketName = System.getenv("WEBSITE_BUCKET_NAME")

        val leagues = getLeagues()
        val parseLeagues = XmlParser().parseLeagues(leagues)

        val store = InMemoryStore()
            .add(parseLeagues.toLeagueInfos())
        val tuesdaySeason = store.getSeason(TUESDAY) ?: 90
        val inputStringTuesday = getInput(TUESDAY, tuesdaySeason);
        val tuesdayLeague = XmlParser().parse(inputStringTuesday)

        val thursdaySeason = store.getSeason(THURSDAY) ?: 90
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

        // store index.html
        val html = printHtml(store.getTeams())
        runBlocking { putObject(websiteBucketName, "index.html", ByteStream.fromString(html), "text/html") }

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
    val timeZone = registry.getTimeZone("Europe/London")
//    val vTimeZone = timeZone.vTimeZone
    val start = ZonedDateTime.ofLocal(fixture.dateTime, timeZone.toZoneId(), ZoneOffset.of("Z"))
    val end = start.plus(fixture.duration)

    val eventName = "${fixture.homeTeam.name} vs ${fixture.awayTeam.name}"

    val meeting: VEvent = VEvent(start, end, eventName)
        .withProperty(timeZone.vTimeZone.timeZoneId)
        .withProperty(uidGenerator.invoke())
        .fluentTarget as VEvent
    return meeting
}

fun printHtml(teams: List<Team>): String {
    val teamsSorted = teams.sortedBy { it.name } // Sort teams alphabetically

    val teamOptions = buildString {
        append("<option value=\"\">-- Select a Team --</option>")
        teamsSorted.forEach { team ->
            append("<option value=\"${team.id}\">${team.name}</option>")
        }
    }

    val html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Team Lookup</title>
            <style>
              body {
                font-family: Arial, sans-serif;
                text-align: center;
                background-color: #f4f4f9;
                margin: 50px;
              }
              .container {
                background: white;
                padding: 20px;
                border-radius: 10px;
                box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                max-width: 400px;
                margin: auto;
                display: flex;
                flex-direction: column;
                align-items: center;
              }
              h1 {
                color: #333;
              }
              select, button {
                margin: 10px;
                padding: 10px;
                font-size: 16px;
                width: calc(100% - 20px);
                border-radius: 5px;
                border: 1px solid #ccc;
                text-align: center;
              }
              button {
                background-color: #28a745;
                color: white;
                border: none;
                cursor: pointer;
              }
              button:hover {
                background-color: #218838;
              }
              #result {
                margin-top: 20px;
                font-size: 20px;
                font-weight: bold;
                color: #555;
                word-break: break-all;
              }
            </style>
        </head>
        <body>
        <div class="container">
            <h1>Team Calendar Lookup</h1>
            <label for="teamSelect">Choose a team:</label>
            <select id="teamSelect">
                $teamOptions
            </select>
            <button onclick="lookupTeam()">Get Calendar!</button>
            <p id="result"></p>
        </div>
        <script>
          function lookupTeam() {
            const select = document.getElementById("teamSelect");
            const result = document.getElementById("result");
            const teamID = select.value;

            if (teamID) {
              const url = `https://spawtz-calendar-calendarbucket-xvug78kqlk9n.s3.eu-west-1.amazonaws.com/calendar/v1/${'$'}{teamID}.ics`;
              result.innerHTML = `<a href="${'$'}{url}" target="_blank">${'$'}{url}</a>`;
            } else {
              result.textContent = "Please select a team.";
            }
          }
        </script>
        </body>
        </html>
    """.trimIndent()

//    println(html) // Or send it as a response from your server.
    return html
}
