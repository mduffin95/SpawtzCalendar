package com.mduffin95.spawtzcalendar.calendar

import com.mduffin95.spawtzcalendar.model.Fixture
import com.mduffin95.spawtzcalendar.model.League
import com.mduffin95.spawtzcalendar.model.LeagueId
import com.mduffin95.spawtzcalendar.model.LeagueInfo
import com.mduffin95.spawtzcalendar.model.SeasonId
import com.mduffin95.spawtzcalendar.model.Team
import com.mduffin95.spawtzcalendar.model.TeamCalendar
import com.mduffin95.spawtzcalendar.model.TeamId
import net.fortuna.ical4j.util.RandomUidGenerator
import java.time.Instant
import java.util.Map

interface FixtureStore {

    fun getFixtures(teamId: TeamId): List<Fixture>

    fun getTeams(): List<Team>

    fun add(league: League): FixtureStore

    fun add(leagueInfos: List<LeagueInfo>): FixtureStore

    fun createCalendarsForTeams(createdInstant: Instant): List<TeamCalendar>

}

private class InMemoryFixtureStore(
    private val teams: MutableMap<TeamId, Team> = mutableMapOf(),
    private val fixturesByTeam: MutableMap<TeamId, List<Fixture>> = mutableMapOf(),
    private val seasonsByLeague: MutableMap<LeagueId, SeasonId> = mutableMapOf()
): FixtureStore {

    val regex = Regex("""^Brighton & Hove.*$""")

    override fun getFixtures(teamId: TeamId): List<Fixture> {
        return fixturesByTeam[teamId].orEmpty()
    }

    override fun getTeams(): List<Team> {
        return teams.values.toList()
    }

    override fun add(league: League): InMemoryFixtureStore {
        val teams = league.fixtures
            .flatMap { listOf(it.homeTeam, it.awayTeam) }
            .filter { it.id != 0 } // empty team
            .fold(mutableMapOf<TeamId, Team>()) { map, team ->
                map[team.id] = team
                map
            }
        this.teams.putAll(teams)

        val fixturesByTeam = league.fixtures
            .flatMap { listOf(Map.entry(it.homeTeam.id, it), Map.entry(it.awayTeam.id, it)) }
            .filter { teams.containsKey(it.key) }
            .groupBy({ it.key }, { it.value })

        this.fixturesByTeam.putAll(fixturesByTeam)
        return this
    }

    override fun add(leagueInfos: List<LeagueInfo>): InMemoryFixtureStore {
        for (leagueInfo in leagueInfos) {
            seasonsByLeague[leagueInfo.id] = leagueInfo.seasonId
            if (regex.matches(leagueInfo.leagueName)) {
                val league = getLeague(leagueInfo.id, leagueInfo.seasonId)
                add(league)
            }
        }

        return this
    }

    override fun createCalendarsForTeams(createdInstant: Instant): List<TeamCalendar> {
        val teams = getTeams()
        val calendarList = mutableListOf<TeamCalendar>()
        val ug = RandomUidGenerator()
        for (team in teams) {
            val fixtures = getFixtures(team.id)
            val teamCalendar = teamCalendar(team, fixtures, createdInstant, ug::generateUid)
            calendarList.add(teamCalendar)
        }
        return calendarList
    }
}

fun getFixtureStore(): FixtureStore {
    return InMemoryFixtureStore()
}
