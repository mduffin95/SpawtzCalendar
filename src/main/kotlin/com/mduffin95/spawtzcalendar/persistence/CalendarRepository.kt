package com.mduffin95.spawtzcalendar.persistence

import aws.smithy.kotlin.runtime.content.ByteStream
import com.mduffin95.spawtzcalendar.calendar.outputCalendar
import com.mduffin95.spawtzcalendar.model.TeamCalendar

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

