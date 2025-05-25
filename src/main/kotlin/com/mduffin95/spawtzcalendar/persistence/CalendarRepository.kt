package com.mduffin95.spawtzcalendar.persistence

import aws.smithy.kotlin.runtime.content.ByteStream
import com.mduffin95.spawtzcalendar.model.TeamCalendar
import net.fortuna.ical4j.data.CalendarOutputter
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

interface CalendarRepository {
    suspend fun store(teamCalendar: TeamCalendar)
}

class S3CalendarRepository(val bucketName: String) : CalendarRepository {

    override suspend fun store(teamCalendar: TeamCalendar) {
        val str = outputCalendar(teamCalendar)
        val byteStream = ByteStream.fromString(str)
        putObject(bucketName, "calendar/v1/${teamCalendar.team.id}.ics", byteStream)
    }

}

fun outputCalendar(teamCalendar: TeamCalendar): String {
    // send the cal reference directly.
    val byteArrayOutputStream = ByteArrayOutputStream()
    val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
    val icsOutputter = CalendarOutputter()
    bufferedOutputStream.use { icsOutputter.output(teamCalendar.calendar, it) }

    return byteArrayOutputStream.toString("UTF-8")
}