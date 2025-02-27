package com.mduffin95

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toJavaLocalDateTime
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
import java.util.stream.Collectors

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val calendarString = parse()

        val bucketName = System.getenv("BUCKET_NAME")

        val byteStream = ByteStream.fromString(calendarString)
        runBlocking { putObject(bucketName, "tag.ics", byteStream) }
        return "OK"
    }

    suspend fun putObject(
        bucketName: String,
        objectKey: String,
        calendarString: ByteStream,
    ) {
//        val metadataVal = mutableMapOf<String, String>()
//        metadataVal["myVal"] = "test"

        val request =
            PutObjectRequest {
                bucket = bucketName
                key = objectKey
//                metadata = metadataVal
                body = calendarString
            }

        S3Client { region = "eu-west-1" }.use { s3 ->
            val response = s3.putObject(request)
            println("Tag information is ${response.eTag}")
        }
    }
}

fun parse(): String {
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

    // send the cal reference directly.
    val byteArrayOutputStream = ByteArrayOutputStream()
    val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
    val icsOutputter = CalendarOutputter()
    bufferedOutputStream.use { icsOutputter.output(cal, it) }

    return byteArrayOutputStream.toString("UTF-8")
    //        return file
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
