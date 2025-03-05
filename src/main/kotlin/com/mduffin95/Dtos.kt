package com.mduffin95

import kotlinx.serialization.Serializable
import net.fortuna.ical4j.model.Calendar
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.time.Duration
import java.time.LocalDateTime

//data class TeamId(val id: String)
typealias TeamId = Int
typealias LeagueId = Int
typealias FixtureId = Int
typealias SeasonId = Int

data class Team(val id: TeamId, val name: String)

data class Fixture(val id: FixtureId, val homeTeam: Team, val awayTeam: Team, val dateTime: LocalDateTime, val duration: Duration)

data class League(
    val id: LeagueId,
    val name: String,
    val fixtures: List<Fixture>
)

data class LeagueInfo(
    val id: LeagueId,
    val leagueName: String,
//    val leagueType: String,
    val divisionId: Int,
    val divisionName: String,
    val seasonId: SeasonId,
    val seasonName: String
)

data class TeamCalendar(val team: Team, val calendar: Calendar)
