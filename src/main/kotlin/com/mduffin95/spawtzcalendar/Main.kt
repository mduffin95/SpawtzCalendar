package com.mduffin95.spawtzcalendar

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.mduffin95.spawtzcalendar.model.Team
import com.mduffin95.spawtzcalendar.model.TeamCalendar
import com.mduffin95.spawtzcalendar.persistence.CalendarRepository
import com.mduffin95.spawtzcalendar.persistence.WebsiteRepository

private class Logger : LambdaLogger {
    override fun log(message: String?) {
        println(message)
    }
    override fun log(message: ByteArray?) {
        TODO("Not yet implemented")
    }

}

private class PrintCalendarRepository : CalendarRepository {
    override suspend fun store(teamCalendar: TeamCalendar) {
        println("Storing calendar for ${teamCalendar.team}")
    }
}

private class PrintWebRepository : WebsiteRepository {
    override fun store(teams: List<Team>) {
        println("Storing website")
    }
}

fun main() {
    generate(PrintCalendarRepository(), PrintWebRepository(), Logger())
}
