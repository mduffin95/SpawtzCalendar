package com.mduffin95

import java.util.Map.entry

class InMemoryStore(val teams: Map<TeamId, Team>, val fixturesByTeam: Map<TeamId, List<Fixture>>): Store {

    override fun getFixtures(teamId: TeamId): List<Fixture> {
        return fixturesByTeam[teamId].orEmpty()
    }

    override fun getTeams(leagueId: LeagueId): List<Team> {
        return teams.values.toList()
    }

    companion object Factory {
        fun fromLeague(league: League): InMemoryStore {
            val teams = league.fixtures
                .flatMap { listOf(it.homeTeam, it.awayTeam) }
                .filter { it.id != 0 } // empty team
                .fold(mutableMapOf<TeamId, Team>()) { map, team ->
                    map[team.id] = team
                    map
                }

            val fixturesByTeam = league.fixtures
                .flatMap { listOf(entry(it.homeTeam.id, it), entry(it.awayTeam.id, it)) }
                .filter { teams.containsKey(it.key) }
                .groupBy({ it.key }, { it.value })

            return InMemoryStore(teams, fixturesByTeam)
        }
    }
}
