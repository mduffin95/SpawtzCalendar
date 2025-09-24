package com.mduffin95.spawtzcalendar

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.mduffin95.spawtzcalendar.calendar.XmlParser
import com.mduffin95.spawtzcalendar.calendar.getFixtureStore
import com.mduffin95.spawtzcalendar.calendar.getLeagues
import com.mduffin95.spawtzcalendar.persistence.CalendarRepository
import com.mduffin95.spawtzcalendar.persistence.S3CalendarRepository
import com.mduffin95.spawtzcalendar.persistence.S3WebsiteRepository
import com.mduffin95.spawtzcalendar.persistence.WebsiteRepository
import kotlinx.coroutines.runBlocking
import java.time.Instant

class Handler: RequestHandler<Map<String, Any>, String> {

    override fun handleRequest(input: Map<String, Any>, context: Context?): String {
        val logger = context?.logger ?: throw IllegalArgumentException()
        logger.log("input map: $input")

        val bucketName = System.getenv("BUCKET_NAME")
        val calRepo : CalendarRepository = S3CalendarRepository(bucketName)

        val websiteBucketName = System.getenv("WEBSITE_BUCKET_NAME")
        val webRepository = S3WebsiteRepository(websiteBucketName)

        generate(calRepo, webRepository, logger)

        return "OK"
    }
}

fun generate(calRepo: CalendarRepository, webRepository: WebsiteRepository, logger: LambdaLogger) {

    logger.log("Getting league information")
    val leagues = getLeagues()
    val parsedLeagues = XmlParser().parseLeagues(leagues)

    logger.log("Building teams")
    val store = getFixtureStore()
        .add(parsedLeagues.toLeagueInfos())

    logger.log("Creating calendar")
    store.createCalendarsForTeams(Instant.now())
        .forEach {
            runBlocking { calRepo.store(it) }
        }

    logger.log("Storing index.html")
    webRepository.store(store.getTeams())

    logger.log("Done!")
}

