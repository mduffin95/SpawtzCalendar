package com.mduffin95

import java.util.Map.entry

class InMemoryStore(
    private val teams: MutableMap<TeamId, Team> = mutableMapOf(),
    private val fixturesByTeam: MutableMap<TeamId, List<Fixture>> = mutableMapOf(),
    private val seasonsByLeague: MutableMap<LeagueId, SeasonId> = mutableMapOf()
): Store {

    override fun getFixtures(teamId: TeamId): List<Fixture> {
        return fixturesByTeam[teamId].orEmpty()
    }

    override fun getTeams(): List<Team> {
        return teams.values.toList()
    }

    override fun getSeason(leagueId: LeagueId): SeasonId? {
        return seasonsByLeague[leagueId]
    }

    fun add(league: League): InMemoryStore {
        val teams = league.fixtures
            .flatMap { listOf(it.homeTeam, it.awayTeam) }
            .filter { it.id != 0 } // empty team
            .fold(mutableMapOf<TeamId, Team>()) { map, team ->
                map[team.id] = team
                map
            }
        this.teams.putAll(teams)

        val fixturesByTeam = league.fixtures
            .flatMap { listOf(entry(it.homeTeam.id, it), entry(it.awayTeam.id, it)) }
            .filter { teams.containsKey(it.key) }
            .groupBy({ it.key }, { it.value })

        this.fixturesByTeam.putAll(fixturesByTeam)
        return this
    }

    fun add(leagueInfos: List<LeagueInfo>): InMemoryStore {
        for (leagueInfo in leagueInfos) {
            seasonsByLeague.put(leagueInfo.id, leagueInfo.seasonId)
        }
        return this
    }
}
