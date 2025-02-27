package com.mduffin95

import net.fortuna.ical4j.model.Calendar
import nl.adaptivity.xmlutil.serialization.XmlElement
import java.time.Duration
import java.time.LocalDateTime

//data class TeamId(val id: String)
typealias TeamId = Int
typealias LeagueId = Int
typealias FixtureId = Int

data class Team(val id: TeamId, val name: String)

data class Fixture(val id: FixtureId, val homeTeam: Team, val awayTeam: Team, val dateTime: LocalDateTime, val duration: Duration)

data class League(
    val id: LeagueId,
    val name: String,
    @XmlElement(true) val fixtures: List<Fixture>
)

data class TeamCalendar(val team: Team, val calendar: Calendar)
