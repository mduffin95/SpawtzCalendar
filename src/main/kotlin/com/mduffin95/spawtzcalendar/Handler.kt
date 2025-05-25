package com.mduffin95.spawtzcalendar

import aws.smithy.kotlin.runtime.content.ByteStream
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.mduffin95.spawtzcalendar.calendar.XmlParser
import com.mduffin95.spawtzcalendar.calendar.createCalendarsForTeams
import com.mduffin95.spawtzcalendar.calendar.getFixtureStore
import com.mduffin95.spawtzcalendar.calendar.getLatestSeasonForLeague
import com.mduffin95.spawtzcalendar.calendar.getLeague
import com.mduffin95.spawtzcalendar.calendar.getLeagues
import com.mduffin95.spawtzcalendar.model.LeagueId
import com.mduffin95.spawtzcalendar.repository.CalendarRepository
import com.mduffin95.spawtzcalendar.repository.S3CalendarRepository
import com.mduffin95.spawtzcalendar.repository.putObject
import getHtml
import kotlinx.coroutines.runBlocking
import java.time.Instant

const val TUESDAY : LeagueId = 1564
const val THURSDAY : LeagueId = 1862

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")
        val websiteBucketName = System.getenv("WEBSITE_BUCKET_NAME")

        logger.log("Getting league information")
        val leagues = getLeagues()
        val parseLeagues = XmlParser().parseLeagues(leagues)

        val store = getFixtureStore()
            .add(parseLeagues.toLeagueInfos())
        logger.log("Getting Tuesday league")
        val tuesdayLeague = store.getSeason(TUESDAY)?.let { getLeague(TUESDAY, it) } ?: getLatestSeasonForLeague(TUESDAY)!!
        logger.log("Getting Thursday league")
        val thursdayLeague = store.getSeason(THURSDAY)?.let { getLeague(THURSDAY, it) } ?: getLatestSeasonForLeague(THURSDAY)!!

        store
            .add(tuesdayLeague)
            .add(thursdayLeague)

        logger.log("Creating calendar")
        val calRepo : CalendarRepository = S3CalendarRepository(bucketName)
        createCalendarsForTeams(store, Instant.now())
            .forEach {
                runBlocking { calRepo.store(it) }
            }

        logger.log("Storing index.html")
        // store index.html
        val html = getHtml(store.getTeams())
        runBlocking { putObject(websiteBucketName, "index.html", ByteStream.fromString(html), "text/html") }

        logger.log("Done!")
        return "OK"
    }
}

